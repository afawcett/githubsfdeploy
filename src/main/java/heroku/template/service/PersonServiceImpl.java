package heroku.template.service;

import heroku.template.model.Person;

import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonServiceImpl implements PersonService {

	@Autowired
	private SessionFactory sessionFactory;
        
    @Transactional
    public void addPerson(Person person) {
    	sessionFactory.getCurrentSession().save(person);
    }

    @Transactional
    public List<Person> listPeople() {

    	return sessionFactory.getCurrentSession().createQuery("from Person").list();
    }

    @Transactional
    public void removePerson(Integer id) {
		Person person = (Person) sessionFactory.getCurrentSession().load(
				Person.class, id);
		if (null != person) {
			sessionFactory.getCurrentSession().delete(person);
		}
    }
    
}
