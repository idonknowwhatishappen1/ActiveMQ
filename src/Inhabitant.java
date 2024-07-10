public class Inhabitant {
    private String name;
    private String dateOfBirth;
    private String maritalStatus;

    public Inhabitant(String name, String dateOfBirth, String maritalStatus) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.maritalStatus = maritalStatus;
    }

    public String getName() {
        return name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }
}
