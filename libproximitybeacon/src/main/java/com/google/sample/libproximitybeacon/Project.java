package com.google.sample.libproximitybeacon;

/**
 * Created by rora on 18.07.2016.
 */
public class Project {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectNamespace() {
        return namespace;
    }

    public void setProjectNamespace(String namespace) {
        this.namespace = namespace;
    }


    private String name;
    private String projectId;
    private String namespace;

    public Project(String name, String projectId){
        this.name = name;
        this.projectId = projectId;
    }

    public Project(String name, String projectId, String namespace){
        this.name = name;
        this.projectId = projectId;
        this.namespace = namespace;
    }
}
