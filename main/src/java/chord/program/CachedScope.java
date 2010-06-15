/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chord.util.FileUtils;
import chord.project.Messages;
import chord.project.Properties;
import chord.util.IndexSet;
import chord.util.ChordRuntimeException;
 
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CachedScope implements IScope {
	private boolean isBuilt = false;
	private IndexSet<jq_Class> classes;
	private IndexSet<jq_Method> methods;
	public IndexSet<jq_Class> getClasses() {
		return classes;
	}
	public IndexSet<jq_Class> getNewInstancedClasses() {
		return null;
	}
	public IndexSet<jq_Method> getMethods() {
		return methods;
	}
	public void build() {
		if (isBuilt)
			return;
		String classesFileName = Properties.classesFileName;
		List<String> classNames = FileUtils.readFileToList(classesFileName);
		classes = Program.loadClasses(classNames);
		Map<String, jq_Class> nameToClassMap = new HashMap<String, jq_Class>();
		for (jq_Class c : classes)
			nameToClassMap.put(c.getName(), c);
		methods = new IndexSet<jq_Method>();
		String methodsFileName = Properties.methodsFileName;
		List<String> methodSigns = FileUtils.readFileToList(methodsFileName);
		for (String s : methodSigns) {
			MethodSign sign = MethodSign.parse(s);
			String cName = sign.cName;
			jq_Class c = nameToClassMap.get(cName);
			if (c == null)
				Messages.log("SCOPE.EXCLUDING_METHOD", s);
			else {
				String mName = sign.mName;
				String mDesc = sign.mDesc;
				jq_Method m = (jq_Method) c.getDeclaredMember(mName, mDesc);
				assert (m != null);
				methods.add(m);
			}
		}
		write(this);
		isBuilt = true;
	}
	public static void write(IScope scope) {
        try {
            PrintWriter out;
            out = new PrintWriter(Properties.classesFileName);
            IndexSet<jq_Class> classes = scope.getClasses();
            for (jq_Class c : classes)
                out.println(c);
            out.close();
            out = new PrintWriter(Properties.methodsFileName);
            IndexSet<jq_Method> methods = scope.getMethods();
            for (jq_Method m : methods)
                out.println(m);
            out.close();
            IndexSet<jq_Class> newInstancedClasses = scope.getNewInstancedClasses();
            out = new PrintWriter(Properties.newInstancedClassesFileName);
			if (newInstancedClasses != null) {
				for (jq_Class c : newInstancedClasses)
					out.println(c);
			}
			out.close();
        } catch (IOException ex) {
            throw new ChordRuntimeException(ex);
        }
    }
}