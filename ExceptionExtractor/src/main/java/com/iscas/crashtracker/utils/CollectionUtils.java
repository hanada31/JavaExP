package com.iscas.crashtracker.utils;

import java.util.*;

public class CollectionUtils {

	public static  int getSizeOfIterator(Iterator it){
		int size = 0;
		while (it.hasNext()) {
			size++;
			it.next();
		}
		return size;
	}
	public static List<Map.Entry<String, Integer>> getTreeMapEntriesSortedByValue(Map<String, Integer> map) {
		List<Map.Entry<String, Integer>> treeMapList =
				new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
		Collections.sort(treeMapList, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o2.getValue() - o1.getValue());
			}
		});
		return treeMapList;
	}
}
