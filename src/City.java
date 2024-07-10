import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class City {
    private String name;
    private List<Inhabitant> inhabitants;

    public City(String name) {
        this.name = name;
        this.inhabitants = new ArrayList<>();
    }

    public void addInhabitant(Inhabitant inhabitant) {
        inhabitants.add(inhabitant);
    }

    public String getAllDOBs() {
        return inhabitants.stream()
                .map(Inhabitant::getDateOfBirth)
                .collect(Collectors.joining(", "));
    }
}
