/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program.reflect;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import chord.util.tuple.object.Pair;
import chord.util.ByteBufferedFile;
import chord.util.ReadException;
import chord.project.analyses.CoreDynamicAnalysis;
import chord.project.Chord;

/**
 * Dynamic analysis for resolving reflection.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DynamicReflectResolver extends CoreDynamicAnalysis {
	private final List<Pair<String, List<String>>> resolvedClsForNameSites =
		new ArrayList<Pair<String, List<String>>>();
	private final List<Pair<String, List<String>>> resolvedObjNewInstSites =
		new ArrayList<Pair<String, List<String>>>();
	private final List<Pair<String, List<String>>> resolvedConNewInstSites =
		new ArrayList<Pair<String, List<String>>>();
	private final List<Pair<String, List<String>>> resolvedAryNewInstSites =
		new ArrayList<Pair<String, List<String>>>();

	public List<Pair<String, List<String>>> getResolvedClsForNameSites() {
		return resolvedClsForNameSites;
	}

	public List<Pair<String, List<String>>> getResolvedObjNewInstSites() {
		return resolvedObjNewInstSites;
	}

	public List<Pair<String, List<String>>> getResolvedConNewInstSites() {
		return resolvedConNewInstSites;
	}

	public List<Pair<String, List<String>>> getResolvedAryNewInstSites() {
		return resolvedAryNewInstSites;
	}

	@Override
	public String getInstrKind() {
		return "online";
	}

	@Override
	public Pair<Class, Map<String, String>> getInstrumentor() {
		return new Pair<Class, Map<String, String>>(ReflectInstrumentor.class, Collections.EMPTY_MAP);
	}

	@Override
	public Pair<Class, Map<String, String>> getEventHandler() {
		return new Pair<Class, Map<String, String>>(ReflectEventHandler.class, Collections.EMPTY_MAP);
	}

	@Override
	public void handleEvent(ByteBufferedFile buffer) throws IOException, ReadException {
		byte opcode = buffer.getByte();
		switch (opcode) {
		case ReflectEventKind.CLS_FOR_NAME_CALL:
		{
			String q = buffer.getString();
			String c = buffer.getString();
			// System.out.println("CLS_FOR_NAME: " + q + " " + c);
			add(resolvedClsForNameSites, q, c);
			break;
		}
		case ReflectEventKind.OBJ_NEW_INST_CALL:
		{
			String q = buffer.getString();
			String c = buffer.getString();
			// System.out.println("OBJ_NEW_INST: " + q + " " + c);
			add(resolvedObjNewInstSites, q, c);
			break;
		}
		case ReflectEventKind.CON_NEW_INST_CALL:
		{
			String q = buffer.getString();
			String c = buffer.getString();
			// System.out.println("CON_NEW_INST: " + q + " " + c);
			add(resolvedConNewInstSites, q, c);
			break;
		}
		case ReflectEventKind.ARY_NEW_INST_CALL:
		{
			String q = buffer.getString();
			String c = buffer.getString();
			// System.out.println("ARY_NEW_INST: " + q + " " + c);
			add(resolvedAryNewInstSites, q, c);
			break;
		}
		default:
			throw new RuntimeException("Unknown opcode: " + opcode);
		}
	}
	private static void add(List<Pair<String, List<String>>> l, String q, String c) {
		for (Pair<String, List<String>> p : l) {
			if (p.val0.equals(q)) {
				List<String> s = p.val1;
				if (!s.contains(c))
					s.add(c);
				return;
			}
		}
		List<String> s = new ArrayList<String>(2);
		s.add(c);
		l.add(new Pair<String, List<String>>(q, s));
	}
}

