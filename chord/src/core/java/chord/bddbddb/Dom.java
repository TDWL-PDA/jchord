/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.bddbddb;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;

import chord.util.IndexHashMap;

/**
 * Generic implementation of a BDD-based domain.
 * <p>
 * Typical usage is as follows:
 * <ul>
 * <li>The domain is initialized by calling {@link #setName(String)}
 * which sets the name of the domain.
 * <li>The domain is next built in memory by repeatedly calling
 * {@link #getOrAdd(Object)} with the argument in each call being a value
 * to be added to the domain.  If the value already exists in the
 * domain then the call does not have any effect.  Otherwise, the
 * value is mapped to integer K in the domain where K is the number
 * of values already in the domain.</li>
 * <li>The domain built in memory is reflected onto disk by calling
 * {@link #save(String)}.</li>
 * <li>The domain on disk can be read by a Datalog program.</li>
 * <li>The domain in memory can be read by calling any of the
 * following:
 * <ul>
 * <li>{@link #iterator()}, which provides an iterator over the
 * values in the domain in memory in the order in which they were
 * added,</li>
 * <li>{@link #get(int)}, which provides the value mapped to the
 * specified integer in the domain in memory, and</li>
 * <li>{@link #indexOf(Object)}, which provides the integer mapped to
 * the specified value in the domain in memory.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @param	<T>	The type of values in the domain.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Dom<T> extends IndexHashMap<T> {
	protected String name;
	public Dom() { }
	public void setName(String name) {
		assert (name != null);
		assert (this.name == null);
		this.name = name;
	}
    public String getName() {
        return name;
    }
	/**
	 * Reflects the domain in memory onto disk.
	 */
	public void save(String dirName) {
		try {
			PrintWriter out;
			String mapFileName = name + ".map";
			out = new PrintWriter(new File(dirName, mapFileName));
			int size = size();
			for (int i = 0; i < size; i++) {
				T val = get(i);
				out.println(toString(val));
			}
			out.close();
			String domFileName = name + ".dom";
			out = new PrintWriter(new File(dirName, domFileName));
			out.println(name + " " + size + " " + mapFileName);
			out.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
    public String toString(T val) {
    	return val == null ? "null" : val.toString();
    }
	/**
	 * Prints the values in the domain in memory to the standard
	 * output stream.
	 */
	public void print() {
		print(System.out);
	}
	/**
	 * Prints the values in the domain in memory to the specified
	 * output stream.
	 * 
	 * @param	out	The output stream to which the values in the
	 * 			domain in memory must be printed.
	 */
	public void print(PrintStream out) {
		for (int i = 0; i < size(); i++)
			out.println(get(i));
	}
	public int hashCode() {
		return System.identityHashCode(this);
	}
	public boolean equals(Object o) {
		return this == o;
	}
}