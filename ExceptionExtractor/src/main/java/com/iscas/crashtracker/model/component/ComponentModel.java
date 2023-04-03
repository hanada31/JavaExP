package com.iscas.crashtracker.model.component;

import com.iscas.crashtracker.model.analyzeModel.AppModel;
import com.iscas.crashtracker.utils.PrintUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ComponentModel
 * 
 * @author 79940
 *
 */
public class ComponentModel implements Serializable {
	private static final long serialVersionUID = 7L;

	protected AppModel appModel;
	protected String componentName;
	protected String exported;
	protected String permission;
	protected List<IntentFilterModel> intentFilterList;
	protected String type;

	public ComponentModel(AppModel appModel) {
		this.appModel = appModel;
		exported = "";
		permission = "";
		intentFilterList = new ArrayList<IntentFilterModel>();
	}

	public Boolean is_mainAct() {
        return appModel.getMainActivity().equals(componentName);
    }

	public Boolean is_exported() {
		boolean action = false;
		for (int i = 0; i < intentFilterList.size(); i++) {
			if (intentFilterList.get(i).getAction_list().size() > 0)
				action = true;
		}
		if (exported != null && exported.equals("true"))
			return true;
		if (exported == null || exported.equals(""))
            return intentFilterList.size() > 0 && action == true;
		return false;
	}

	@Override
	public String toString() {

		String res = "";
		res += "componentName: " + componentName + "\n";
		res += "exported: " + exported + "\n";
		res += "permission: " + permission + "\n";
		res += "intentFilterList: " + PrintUtils.printList(intentFilterList) + "\n";

		return res;
	}

	public AppModel getAppModel() {
		return appModel;
	}

	public String getComponetName() {
		return componentName;
	}

	public void setComponetName(String activityName) {
		this.componentName = activityName;
	}

	public String getExported() {
		return exported;
	}

	public void setExported(String exported) {
		this.exported = exported;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getComponentType() {
		return "unkown";
	}

	public void setComponentType(String type) {
		this.type = type;
	}

	public List<IntentFilterModel> getIntentFilters() {
		return intentFilterList;
	}

	public void addIntentFilter(IntentFilterModel filterModel) {
		this.intentFilterList.add(filterModel);
	}

}
