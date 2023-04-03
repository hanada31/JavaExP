package com.iscas.crashtracker.client.manifest;

import com.iscas.crashtracker.base.Analyzer;
import com.iscas.crashtracker.base.Global;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.model.analyzeModel.AppModel;
import com.iscas.crashtracker.model.component.*;
import com.iscas.crashtracker.utils.SootUtils;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


/**
 * This class is used to parse a manifest XML file Extract all the exported
 * components (Activity, Service, and Receiver)
 *
 * @author hanada
 */
public class ManifestAnalyzer extends Analyzer {
    private ProcessManifest manifestManager;

    public ManifestAnalyzer() {
        super();
    }

    /**
     * get information from manifest file e.g., activity list, exported
     * attributes
     */
    @Override
    public void analyze() {
        try {
            manifestManager = new ProcessManifest(appModel.getAppPath());
            appModel.setManifestString(getManifestString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("no manifest is available.");
        }
        if (manifestManager == null)
            return;
        String pkg = manifestManager.getPackageName();
        // the ".debug" will cause the imprecise of -exlib
        if (pkg.endsWith(".debug"))
            pkg = pkg.replace(".debug", "");
        appModel.setPackageName(pkg);
        appModel.getExtendedPakgs().add(pkg);
        appModel.setVersionCode(manifestManager.getVersionCode());
        appModel.setUsesPermissionSet(manifestManager.getPermissions());

        // -------------------------------------------------------------------------------------------------------------
        // BUGFIX: 09/06/2022 LightningRS
        // -------------------------------------------------------------------------------------------------------------
        // We found that EagerComponentContainer (since FlowDroid ~2.10) will filter out the component with the same
        // name, which will cause multiple AXmlNodes of the same component to be lost. Thus, we use AXmlHandler
        // to get nodes directly instead of using getXXX() methods provided by ManifestAnalyzer.
        // -------------------------------------------------------------------------------------------------------------
        List<AXmlNode> activityNodes = new ArrayList<>(manifestManager.getAXml().getNodesWithTag("activity"));
        parseComponent(activityNodes, "Activity");

        List<AXmlNode> serviceNodes = new ArrayList<>(manifestManager.getAXml().getNodesWithTag("service"));
        parseComponent(serviceNodes, "Service");

        List<AXmlNode> providerNodes = new ArrayList<>(manifestManager.getAXml().getNodesWithTag("provider"));
        parseComponent(providerNodes, "Provider");

        List<AXmlNode> receiverNodes = new ArrayList<>(manifestManager.getAXml().getNodesWithTag("receiver"));
        parseComponent(receiverNodes, "Receiver");

        mergeAllComponents();

        List<AXmlNode> alis = manifestManager.getAliasActivities();
        for (AXmlNode actNode : alis) {
            AXmlNode targetNode = manifestManager.getAliasActivityTarget(actNode);
            if (targetNode == null) {
                continue;
            }
            String name = targetNode.getAttribute("name").getValue().toString();
            if (appModel.getActivityMap().containsKey(name)) {
                ActivityModel actModel = (ActivityModel) appModel.getActivityMap().get(name);
                analyzeIntentFilter(actModel, actNode);
            }
        }

        // get EAs
        for (ComponentModel component : appModel.getComponentMap().values()) {
            if (component.is_exported()) {
                appModel.getExportedComponentMap().put(component.getComponetName(), component);
            }
        }
        MyConfig.getInstance().setManifestAnalyzeFinish(true);
    }

    /**
     * parse activity + service + contentProvider + broadcastReceiver node in
     * manifest
     */
    private void parseComponent(List<AXmlNode> components, String type) {
        // get components
        HashMap<String, ComponentModel> componentMap = getComponentMap(type);
        for (AXmlNode componentNode : components) {
            // ignore the component whose "enabled" attribute is "false"
            if (componentNode.hasAttribute("enabled")) {
                if (componentNode.getAttribute("enabled").getValue().toString().equals("")
                        || componentNode.getAttribute("enabled").getValue().toString().equals("false")) {
                    continue;
                }
            }

            // new ActivityData instance
            String componentName = componentNode.getAttribute("name").getValue().toString();
            if (!Global.v().getAppModel().getApplicationClassNames().contains(componentName)) {
                if (!componentName.contains(appModel.getPackageName())) {
                    if (componentName.startsWith(".")) {
                        componentName = appModel.getPackageName() + componentName;
                    } else {
                        componentName = appModel.getPackageName() + "." + componentName;
                    }
                }
            }
            // Exclude the activities not declared in app's package.
            // if (!MyConfig.getInstance().getMySwitch().isLibCodeSwitch()&&
            // !componentName.contains(appModel.getPackageName())) // out of
            // package
            // continue;

            // add external libs according to component declaration
            if (!componentName.contains(appModel.getPackageName())) {
                String[] ss = componentName.split("\\.");
                if (ss.length >= 2)
                    appModel.getExtendedPakgs().add(ss[0] + "." + ss[1]);
            }
            ComponentModel componentModel = getComponentModel(type, componentName);
            if (componentModel == null)
                continue;
            componentModel.setComponetName(componentName);

            synchronized (Objects.requireNonNull(componentMap)) {
                if (!componentMap.containsKey(componentModel.getComponetName())) {
                    componentMap.put(componentModel.getComponetName(), componentModel);
                }
            }

            // get the attributes of the activity element
            if (componentNode.getAttribute("exported") != null)
                componentModel.setExported(componentNode.getAttribute("exported").getValue().toString());

            if (componentNode.getAttribute("permission") != null)
                componentModel.setPermission(componentNode.getAttribute("permission").getValue().toString());

            if (componentModel instanceof ActivityModel && componentNode.getAttribute("launchMode") != null)
                ((ActivityModel) componentModel).setLaunchMode(componentNode.getAttribute("launchMode").getValue()
                        .toString());

            if (componentModel instanceof ActivityModel && componentNode.getAttribute("taskAffinity") != null)
                ((ActivityModel) componentModel).setTaskAffinity(componentNode.getAttribute("taskAffinity").getValue()
                        .toString());

            else if (componentModel instanceof ActivityModel)
                ((ActivityModel) componentModel).setTaskAffinity(appModel.getPackageName());

            analyzeIntentFilter(componentModel, componentNode);
        }

    }

    /**
     * merge All Components
     */
    private void mergeAllComponents() {
        appModel.setComponentMap(appModel.getActivityMap());
        appModel.setComponentMap(appModel.getServiceMap());
        appModel.setComponentMap(appModel.getProviderMap());
        appModel.setComponentMap(appModel.getRecieverMap());
    }

    /**
     * getComponentMap for type
     *
     * @param type
     * @return
     */
    private HashMap<String, ComponentModel> getComponentMap(String type) {
        switch (type) {
            case "Activity":
                return appModel.getActivityMap();
            case "Service":
                return appModel.getServiceMap();
            case "Provider":
                return appModel.getProviderMap();
            case "Receiver":
                return appModel.getRecieverMap();
        }
        return null;
    }

    /**
     * getComponentModel for type
     *
     * @param type
     * @param componentName
     * @return
     */
    private ComponentModel getComponentModel(String type, String componentName) {
        if (SootUtils.getSootClassByName(componentName) == null)
            return null;
        switch (type) {
            case "Activity":
                if (appModel.getActivityMap().containsKey(componentName))
                    return appModel.getActivityMap().get(componentName);
                return new ActivityModel(appModel);
            case "Service":
                if (appModel.getServiceMap().containsKey(componentName))
                    return appModel.getServiceMap().get(componentName);
                return new ServiceModel(appModel);
            case "Receiver":
                if (appModel.getRecieverMap().containsKey(componentName))
                    return appModel.getRecieverMap().get(componentName);
                return new BroadcastReceiverModel(appModel);
//            case "Provider": //do not add coontent provider
//                if (appModel.getProviderMap().containsKey(componentName))
//                    return appModel.getProviderMap().get(componentName);
//                return new ContentProviderModel(appModel);
        }
        return null;
    }

    private String getManifestString() {
        return manifestManager.getAXml().toString();
    }

    public AppModel getAppMode() {
        return appModel;
    }

    /**
     * analcomponent each intent-filter
     *
     * @param componentModel
     * @param componentNode
     */
    private void analyzeIntentFilter(ComponentModel componentModel, AXmlNode componentNode) {
        // traverse the child elements of the activity element
        for (AXmlNode ifNode : componentNode.getChildren()) {
            if (ifNode.getTag().equals("intent-filter")) {
                // new IntentFilterData instance
                IntentFilterModel ifd = new IntentFilterModel();
                componentModel.addIntentFilter(ifd);
                if (ifNode.getAttribute("priority") != null)
                    ifd.setPriority(ifNode.getAttribute("priority").getValue().toString());

                // traverse child elements of the intent filter element
                for (AXmlNode childNode : ifNode.getChildren()) {
                    if (childNode.getTag().equals("action")) {
                        String action = childNode.getAttribute("name").getValue().toString();
                        ifd.getAction_list().add(action);
                    }
                    if (childNode.getTag().equals("category")) {
                        String category = childNode.getAttribute("name").getValue().toString();
                        ifd.getCategory_list().add(category);
                    }
                    if (ifd.getAction_list().contains("android.intent.action.MAIN")
                            && ifd.getCategory_list().contains("android.intent.category.LAUNCHER")) {
                        appModel.setMainActivity(componentModel.getComponetName());
                    }

                    if (childNode.getTag().equals("data")) {
                        Data data = new Data();
                        if (childNode.getAttribute("scheme") != null)
                            data.setScheme(childNode.getAttribute("scheme").getValue().toString());
                        if (childNode.getAttribute("authority") != null)
                            data.setAuthority(childNode.getAttribute("authority").getValue().toString());
                        if (childNode.getAttribute("path") != null)
                            data.setPath(childNode.getAttribute("path").getValue().toString());
                        if (childNode.getAttribute("mimeType") != null)
                            ifd.getDatatype_list().add(childNode.getAttribute("mimeType").getValue().toString());
                        if (data.toString().length() > 0)
                            ifd.getData_list().add(data);
                    }
                }
            }
        }
    }
}
