package com.techsure.autoexecrunner.codehub.utils;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * @author YuJH
 *
 *         内存分页工具, 统一调用, 避免在api中手写一堆重复的代码, 主要用于proxy返回的数据封装, 用法可以参考commitlog搜索相关api
 * @param <T>
 */
public class PageHelper<T> {


	private List<T> data;

	private int pageSize = 10;
	private int currentPage = 1;
	private boolean needPage = true;

	public PageHelper(List<T> data, int pageSize) {
		this.data = data;
		this.pageSize = pageSize;
	}

	public PageHelper(List<T> data, JSONObject paramJsonObject) {
		this.data = data;
		if (paramJsonObject.containsKey("currentPage")) {
			this.currentPage = paramJsonObject.getIntValue("currentPage");
		}
		if (paramJsonObject.containsKey("pageSize")) {
			this.pageSize = paramJsonObject.getIntValue("pageSize");
		}
		if (paramJsonObject.containsKey("needPage")) {
			this.needPage = paramJsonObject.getBooleanValue("needPage");
		}
	}

	public int getPageCount() {
		if (pageSize == 0) {
			return 0;
		}
		return data.size() % pageSize == 0 ? (data.size() / pageSize) : (data.size() / pageSize + 1);
	}

	public Iterator<List<T>> iterator() {
		return new Itr();
	}

	public List<T> page(int pageNum) {
		if (pageNum < 1) {
			pageNum = 1;
		}
		int from = (pageNum - 1) * pageSize;
		int to = Math.min(pageNum * pageSize, data.size());
		if (from > to) {
			from = to;
		}
		return data.subList(from, to);
	}

	public JSONObject pageResult() {

		JSONObject resultObj = new JSONObject();
		if (needPage) {

			resultObj.put("pageSize", pageSize);
			resultObj.put("pageCount", getPageCount());
			resultObj.put("currentPage", currentPage);
			resultObj.put("rowNum", data.size());
		}
		resultObj.put("list", page(currentPage));

		return resultObj;
	}
	
	private class Itr implements Iterator<List<T>> {
		int page = 1;

		Itr() {
		}

		@Override
		public boolean hasNext() {
			return page <= getPageCount();
		}

		@Override
		public List<T> next() {
			int i = page;
			if (i > getPageCount())
				return new ArrayList<>();

			page = i + 1;
			return PageHelper.this.page(i);
		}
	}

}