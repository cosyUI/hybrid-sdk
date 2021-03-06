package com.wya.hybrid.methods.bean.openwin.bean;

import android.app.Activity;

/**
 * @date: 2019/2/18 15:08
 * @author: Chunjiang Mao
 * @classname: ActivityData
 * @describe:
 */
public class ActivityData {
	/**
	 * 窗口名字
	 */
	private String name;
	/**
	 * 保存的activity对象
	 */
	private Activity activity;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}
}
