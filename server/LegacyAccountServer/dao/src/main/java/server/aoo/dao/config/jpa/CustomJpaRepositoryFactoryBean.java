package server.aoo.dao.config.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import server.aoo.dao.config.util.CustomerDaoImpl;

import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 * 自定义JpaRepositoryFactoryBean
 */
public class CustomJpaRepositoryFactoryBean<R extends JpaRepository<T, ID>, T, ID extends Serializable> extends
        JpaRepositoryFactoryBean<R, T, ID> {

	public CustomJpaRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new CustomJpaExecutorFactory(entityManager);
	}

	/**
	 * 自定义 jpa executor factory
	 * 
	 * @param <T>
	 * @param <I>
	 */
	private static class CustomJpaExecutorFactory<T, I extends Serializable> extends JpaRepositoryFactory {

		/**
		 * Simple jpa executor factory constructor
		 * 
		 * @param entityManager
		 *            entity manager
		 */
		public CustomJpaExecutorFactory(EntityManager entityManager) {
			super(entityManager);
		}

		@Override
		protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
			return CustomerDaoImpl.class;
		}

		@Override
		protected JpaRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information, EntityManager entityManager) {
			return new CustomerDaoImpl(information.getDomainType(), entityManager);
		}
	}
}