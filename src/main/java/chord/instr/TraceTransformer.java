/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.instr;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import gnu.trove.TByteArrayList;

import chord.project.Properties;
import chord.runtime.Runtime;
import chord.util.ChordRuntimeException;
import chord.util.ByteBufferedFile;
import chord.util.ReadException;
import chord.util.tuple.integer.IntTrio;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class TraceTransformer {
	private final String rdFileName;
	private final String wrFileName;

	// cached values from scheme for efficiency
	private final boolean newAndNewArrayHasHid;
	private final boolean newAndNewArrayHasTid;
	private final boolean newAndNewArrayHasOid;
	private final int newAndNewArrayNumBytes;
	private final int getstaticPrimitiveNumBytes;
	private final int getstaticReferenceNumBytes;
	private final int putstaticPrimitiveNumBytes;
	private final int putstaticReferenceNumBytes;
	private final int getfieldPrimitiveNumBytes;
	private final int getfieldReferenceNumBytes;
	private final int putfieldPrimitiveNumBytes;
	private final int putfieldReferenceNumBytes;
	private final int aloadPrimitiveNumBytes;
	private final int aloadReferenceNumBytes;
	private final int astorePrimitiveNumBytes;
	private final int astoreReferenceNumBytes;
	private final int threadStartNumBytes;
	private final int threadJoinNumBytes;
	private final int acquireLockNumBytes;
	private final int releaseLockNumBytes;
	private final int waitNumBytes;
	private final int notifyNumBytes;
	private final int methodCallNumBytes;
	private final int returnPrimitiveNumBytes;
	private final int returnReferenceNumBytes;
	private final int explicitThrowNumBytes;
	private final int implicitThrowNumBytes;

	private ByteBufferedFile reader, writer;
	private byte[] tmp;
	private List<IntTrio> pending;
	private int count;

	public TraceTransformer(String rdFileName, String wrFileName, InstrScheme scheme) {
		this.rdFileName = rdFileName;
		this.wrFileName = wrFileName;
		newAndNewArrayHasHid = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY).hasLoc();
		newAndNewArrayHasTid = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY).hasThr();
		newAndNewArrayHasOid = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY).hasObj();
		assert (newAndNewArrayHasOid);
		newAndNewArrayNumBytes = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY).size();
		getstaticPrimitiveNumBytes = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE).size();
		getstaticReferenceNumBytes = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE).size();
		putstaticPrimitiveNumBytes = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE).size();
		putstaticReferenceNumBytes = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE).size();
		getfieldPrimitiveNumBytes = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE).size();
		getfieldReferenceNumBytes = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE).size();
		putfieldPrimitiveNumBytes = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE).size();
		putfieldReferenceNumBytes = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE).size();
		aloadPrimitiveNumBytes = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE).size();
		aloadReferenceNumBytes = scheme.getEvent(InstrScheme.ALOAD_REFERENCE).size();
		astorePrimitiveNumBytes = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE).size();
		astoreReferenceNumBytes = scheme.getEvent(InstrScheme.ASTORE_REFERENCE).size();
		threadStartNumBytes = scheme.getEvent(InstrScheme.THREAD_START).size();
		threadJoinNumBytes = scheme.getEvent(InstrScheme.THREAD_JOIN).size();
		acquireLockNumBytes = scheme.getEvent(InstrScheme.ACQUIRE_LOCK).size();
		releaseLockNumBytes = scheme.getEvent(InstrScheme.RELEASE_LOCK).size();
		waitNumBytes = scheme.getEvent(InstrScheme.WAIT).size();
		notifyNumBytes = scheme.getEvent(InstrScheme.NOTIFY).size();
		methodCallNumBytes = scheme.getEvent(InstrScheme.METHOD_CALL).size();
		returnPrimitiveNumBytes = scheme.getEvent(InstrScheme.RETURN_PRIMITIVE).size();
		returnReferenceNumBytes = scheme.getEvent(InstrScheme.RETURN_REFERENCE).size();
		explicitThrowNumBytes = scheme.getEvent(InstrScheme.EXPLICIT_THROW).size();
		implicitThrowNumBytes = scheme.getEvent(InstrScheme.IMPLICIT_THROW).size();
	}
	public void run() {
		try {
			reader = new ByteBufferedFile(Properties.traceBlockSize, rdFileName, true);
			writer = new ByteBufferedFile(Properties.traceBlockSize, wrFileName, false);
			pending = new ArrayList<IntTrio>();
 			tmp = new byte[100000];
			count = 0; // size of tmp
			while (!reader.isDone()) {
				if (count != 0) {
					if (count > 50000000) {
						System.out.print("WARNING: size: " + count + " PENDING:");
						for (int i = 0; i < pending.size(); i++) {
							IntTrio trio = pending.get(i);
							System.out.print(" " + trio.idx2 + ":" + trio.idx0);
						}
						System.out.println();
						// remove 1st item in pending, it is oldest
						pending.remove(0);
						adjust();
					}
				}
				byte opcode = reader.getByte();
				switch (opcode) {
				case EventKind.BEF_NEW:
				{
					addToTmp(EventKind.NEW);
					byte hIdx1 = reader.getByte();
					byte hIdx2 = reader.getByte();
					byte hIdx3 = reader.getByte();
					byte hIdx4 = reader.getByte();
					byte tIdx1 = reader.getByte();
					byte tIdx2 = reader.getByte();
					byte tIdx3 = reader.getByte();
					byte tIdx4 = reader.getByte();
					if (newAndNewArrayHasHid) {
						addToTmp(hIdx1);
						addToTmp(hIdx2);
						addToTmp(hIdx3);
						addToTmp(hIdx4);
					}
					if (newAndNewArrayHasTid) {
						addToTmp(tIdx1);
						addToTmp(tIdx2);
						addToTmp(tIdx3);
						addToTmp(tIdx4);
					}
					int hIdx = ByteBufferedFile.assemble(hIdx1, hIdx2, hIdx3, hIdx4);
					int tIdx = ByteBufferedFile.assemble(tIdx1, tIdx2, tIdx3, tIdx4);
					pending.add(new IntTrio(hIdx, tIdx, count));
					addToTmp((byte) 0); // dummy placeholder for obj
					addToTmp((byte) 0); // dummy placeholder for obj
					addToTmp((byte) 0); // dummy placeholder for obj
					addToTmp((byte) 0); // dummy placeholder for obj
					break;
				} 
				case EventKind.AFT_NEW:
				{
					int hIdx = reader.getInt();
					int tIdx = reader.getInt();
					byte oIdx1 = reader.getByte();
					byte oIdx2 = reader.getByte();
					byte oIdx3 = reader.getByte();
					byte oIdx4 = reader.getByte();
					int n = pending.size();
					for (int i = 0; i < n; i++) {
						IntTrio trio = pending.get(i);
						if (trio.idx0 == hIdx && trio.idx1 == tIdx) {
							int j = trio.idx2; 
							tmp[j] = oIdx1;
							tmp[j + 1] = oIdx2;
							tmp[j + 2] = oIdx3;
							tmp[j + 3] = oIdx4;
							pending.remove(i);
							if (i == 0)
								adjust();
							break;
						}
					}
					break;
				}
				default:
				{
					int offset = getOffset(opcode);
					if (count != 0) {
						addToTmp(opcode);
						for (int i = 0; i < offset; i++) {
							byte v = reader.getByte();
							addToTmp(v);
						}
					} else {
						writer.putByte(opcode);
						for (int i = 0; i < offset; i++) {
							byte v = reader.getByte();
							writer.putByte(v);
						}
					}
					break;
				}
				}
			}
			if (count != 0) {
				System.out.println("WARNING: pending list:");
				for (int i = 0; i < pending.size(); i++) {
					IntTrio trio = pending.get(i);
					System.out.println("\thIdx: " + trio.idx0 + " tIdx: " + trio.idx1);
				}
				for (int i = 0; i < count; i++) {
					byte v = tmp[i];
					writer.putByte(v);
				}
			} else
				assert (pending.size() == 0);
			writer.flush();
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		} catch (ReadException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	private int getOffset(int opcode) {
		switch (opcode) {
		case EventKind.NEW:
		case EventKind.NEW_ARRAY:
			return newAndNewArrayNumBytes;
		case EventKind.GETSTATIC_PRIMITIVE:
			return getstaticPrimitiveNumBytes;
		case EventKind.GETSTATIC_REFERENCE:
			return getstaticReferenceNumBytes;
		case EventKind.PUTSTATIC_PRIMITIVE:
			return putstaticPrimitiveNumBytes;
		case EventKind.PUTSTATIC_REFERENCE:
			return putstaticReferenceNumBytes;
		case EventKind.GETFIELD_PRIMITIVE:
			return getfieldPrimitiveNumBytes;
		case EventKind.GETFIELD_REFERENCE:
			return getfieldReferenceNumBytes;
		case EventKind.PUTFIELD_PRIMITIVE:
			return putfieldPrimitiveNumBytes;
		case EventKind.PUTFIELD_REFERENCE:
			return putfieldReferenceNumBytes;
		case EventKind.ALOAD_PRIMITIVE:
			return aloadPrimitiveNumBytes;
		case EventKind.ALOAD_REFERENCE:
			return aloadReferenceNumBytes;
		case EventKind.ASTORE_PRIMITIVE:
			return astorePrimitiveNumBytes;
		case EventKind.ASTORE_REFERENCE:
			return astoreReferenceNumBytes;
		case EventKind.ENTER_METHOD:
		case EventKind.LEAVE_METHOD:
		case EventKind.ENTER_LOOP:
		case EventKind.LEAVE_LOOP:
			return 8;
		case EventKind.METHOD_CALL_BEF:
		case EventKind.METHOD_CALL_AFT:
			return methodCallNumBytes;
		case EventKind.RETURN_PRIMITIVE:
			return returnPrimitiveNumBytes;
		case EventKind.RETURN_REFERENCE:
			return returnReferenceNumBytes;
		case EventKind.EXPLICIT_THROW:
			return explicitThrowNumBytes;
		case EventKind.IMPLICIT_THROW:
			return implicitThrowNumBytes;
		case EventKind.QUAD:
		case EventKind.BASIC_BLOCK:
			return 8;
		case EventKind.THREAD_START:
			return threadStartNumBytes;
		case EventKind.THREAD_JOIN:
			return threadJoinNumBytes;
		case EventKind.ACQUIRE_LOCK:
			return acquireLockNumBytes;
		case EventKind.RELEASE_LOCK:
			return releaseLockNumBytes;
		case EventKind.WAIT:
			return waitNumBytes;
		case EventKind.NOTIFY:
		case EventKind.NOTIFY_ALL:
			return notifyNumBytes;
		default:
			throw new ChordRuntimeException();
		}
	}
	private void adjust() throws IOException {
		int limit;
		int pendingSize = pending.size();
		if (pendingSize == 0)
			limit = count;
		else {
			IntTrio trio = pending.get(0);
			limit = trio.idx2;
			trio.idx2 = 0;
			for (int i = 1; i < pendingSize; i++) {
				trio = pending.get(i);
				trio.idx2 -= limit;
			}
		}
		int j = 0;
		for (; j < limit; j++) {
			byte v = tmp[j];
			writer.putByte(v);
		}
		int i = 0;
		for (; j < count; j++) {
			tmp[i++] = tmp[j];
		}
		count -= limit;
	}
	private void addToTmp(byte v) {
		int n = tmp.length;
		if (count == n) {
			byte[] tmp2 = new byte[n * 2];
			System.arraycopy(tmp, 0, tmp2, 0, n);
			tmp = tmp2;
		}
		tmp[count++] = v;
	}
}
