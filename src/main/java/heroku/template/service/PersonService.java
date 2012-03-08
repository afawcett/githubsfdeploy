package heroku.template.service;

import heroku.template.model.Person;

import java.util.List;

public interface PersonService {
    
    public void addPerson(Person person);
    public List<Person> listPeople();
    public void removePerson(Integer id);
}
