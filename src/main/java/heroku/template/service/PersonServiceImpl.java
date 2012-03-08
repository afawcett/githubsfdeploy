package heroku.template.service;

import heroku.template.model.Person;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonServiceImpl implements PersonService {

    @PersistenceContext
    EntityManager em;
        
    @Transactional
    public void addPerson(Person person) {
    	em.persist(person);
    }

    @Transactional
    public List<Person> listPeople() {
    	return em.createQuery("from Person").getResultList();
    }

    @Transactional
    public void removePerson(Integer id) {
		Person person = (Person) em.find(Person.class, id);
		if (null != person) {
			em.remove(person);
		}
    }
    
}
