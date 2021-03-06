/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.alloc.path;

import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class RelevantAnalysis {
	private static int[] def;
	private static int[][] defs;
	private static int[] use;
	private static int[][] uses;
	private static int[][] movs;
	private static int[][] live;
	private static boolean changed;
	private static final int REDIRECTED = ThreadEscapePathAnalysis.REDIRECTED;
	private static final int NULL_U_VAL = ThreadEscapePathAnalysis.NULL_U_VAL;
	private static final int NULL_Q_VAL = ThreadEscapePathAnalysis.NULL_Q_VAL;
	private static final int AVG_LIVE_VARS_ESTIMATE = 4;
	public static void run(int[] def2, int[][] defs2,
			int[] use2, int[][] uses2, int[][] movs2,
			int[] succ, int[][] succs, int numQ) {
		System.out.println("ENTER RelevantAnalysis");
		def = def2;
		defs = defs2;
		use = use2;
		uses = uses2;
		movs = movs2;
		live = new int[numQ][];
		for (int q = 0; q < numQ; q++)
			live[q] = new int[AVG_LIVE_VARS_ESTIMATE + 1];
		changed = true;
		for (int iter = 0; changed; iter++) {
			System.out.println("ITERATION: " + iter);
			changed = false;
			for (int q = numQ - 1; q >= 0; q--) {
				final int r = succ[q];
				if (r != NULL_Q_VAL) {
					if (r != REDIRECTED)
						process(q, r);
					else {
						final int[] S = succs[q];
						final int nS = S[0];
						for (int i = 1; i <= nS; i++)
							process(q, S[i]);
					}
				}
			}
		}
		System.out.println("LEAVE RelevantAnalysis");

		long numLive = 0;
		for (int q = 0; q < numQ; q++)
			numLive += live[q][0];
		System.out.println("NUM LIVE: " + numLive);

        ProgramRel liveQU_o = (ProgramRel) ClassicProject.g().getTrgt("liveQU_o");
        liveQU_o.zero();
        for (int q = 0; q < numQ; q++) {
            final int[] Q = live[q];
            final int nQ = Q[0];
            for (int i = 1; i <= nQ; i++)
                liveQU_o.add(q, Q[i]);
        }
        liveQU_o.save();
	}
	private static void process(int q, int r) {
		int[] Q = live[q];
		final int[] origQ = Q;

		// live[q] must contain all in use[r]
		final int u = use[r];
		if (u != NULL_U_VAL) {
			if (u != REDIRECTED)
				Q = addIfAbsent(u, Q);
			else {
				final int[] U = uses[r];
				final int nU = U[0];
				for (int i = 1; i <= nU; i++)
					Q = addIfAbsent(U[i], Q);
			}
		}

		// live[q] must contain all in (live[r] - def[r])
		final int[] R = live[r];
		final int nR = R[0];
		final int d = def[r];
		if (d == NULL_U_VAL) {
			for (int i = 1; i <= nR; i++)
				Q = addIfAbsent(R[i], Q);
		} else if (d != REDIRECTED) {
			for (int i = 1; i <= nR; i++) {
				final int l = R[i];
				if (d != l)
					Q = addIfAbsent(l, Q);
			}
		} else {
			final int[] D = defs[r];
			final int nD = D[0];
			for (int i = 1; i <= nR; i++) {
				final int l = R[i];
				boolean found = false;
				for (int j = 1; j <= nD; j++) {
					if (D[j] == l) {
						found = true;
						break;
					}
				}
				if (!found)
					Q = addIfAbsent(l, Q);
			}
		}

		// for each (r,l,v) in movs: live[q] must contain
		// v if live[r] contains l
		final int[] M = movs[r];
		if (M != null) {
			final int nM = M[0];
			int j = 1;
			for (int i = 1; i <= nM; i++) {
				final int l = M[j];
				boolean found = false;
				for (int k = 1; k <= nR; k++) {
					if (R[k] == l) {
						found = true;
						break;
					}
				}
				if (found)
					Q = addIfAbsent(M[j + 1], Q);
				j += 2;
			}
		}

		if (Q != origQ)
			live[q] = Q;
	}

	private static int[] addIfAbsent(int v, int[] l) {
		final int n = l[0];
		for (int i = 1; i <= n; i++) {
			if (l[i] == v)
				return l;
		}
		int len = l.length;
		if (n == len - 1) {
			int[] l2 = new int[len * 2];
			System.arraycopy(l, 0, l2, 0, len);
			l = l2;
		}
		l[++l[0]] = v; 
		changed = true;
		return l;
	}
}
