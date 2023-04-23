package com.iscas.exceptionextractor.utils;

import com.iscas.exceptionextractor.model.analyzeModel.ConditionWithValueSet;

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
	public static void add_rfcondition_to_map(String key, ConditionWithValueSet attr, Map<String, ConditionWithValueSet> map) {
		if (map == null || key == null)
			return;
		if (!map.containsKey(key)) {
			map.put(key, attr);
		}
	}

	public static Object getLastItemInList(List collections){
		if(collections == null ||collections.size()==0) return  null;
		return collections.get(collections.size()-1);

	}

	/**
	 * 输入两个arraylist,返回第一个公共元素
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static <T> T getCommonElement(List<T> list1, List<T> list2) {
		HashSet<T> set = new HashSet<T>(list1);
		for (int i = 0; i < list2.size(); i++) {
			T element = list2.get(i);
			if (set.contains(element)) {
				return element;
			}
		}
		return null; // 如果没有公共元素，返回null
	}
}
