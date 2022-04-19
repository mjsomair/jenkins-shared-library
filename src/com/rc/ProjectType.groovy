enum ProjectType {
    APPLICATION("application"), AUTOTESTS("autotests"), LIBRARY("library")

    private String value

    ProjectType(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }
}
