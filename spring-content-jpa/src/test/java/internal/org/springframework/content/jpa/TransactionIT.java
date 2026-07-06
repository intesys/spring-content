package internal.org.springframework.content.jpa;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

class TransactionIT {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private PlatformTransactionManager ptm;

	private TestEntityRepository repo = null;
	private TestEntityContentRepository store = null;
	private DbService dbService = null;

	private TestEntity te = null;

	static Stream<Arguments> configs() {
		return Stream.of(
			Arguments.of(H2Config.class, "H2"),
			Arguments.of(HSQLConfig.class, "HSQL"),
			Arguments.of(MySqlConfig.class, "MySQL"),
			Arguments.of(PostgresConfig.class, "Postgres"));
			//Arguments.of(StoreIT.SqlServerConfig.class, "SQLServer"),
			//Arguments.of(StoreIT.OracleConfig.class, "Oracle"));
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("configs")
	void transactionTest(Class<?> configClass, String name) {
		context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.register(configClass);
		context.refresh();

		repo = context.getBean(TestEntityRepository.class);
		store = context.getBean(TestEntityContentRepository.class);
		dbService = context.getBean(DbService.class);
		ptm = context.getBean(PlatformTransactionManager.class);

		te = new TestEntity();
		te = repo.save(te);
		assertThat(te.getId(), is(not(nullValue())));
		assertThat(te.getContentId(), is(nullValue()));

		try {
			try {
				te = dbService.doSomeDbStuff(store, te);
			} catch (Exception e) {
				ContentStoreIT.doInTransaction(ptm, () -> {
					try (InputStream result = store.getContent(te)) {
						assertThat(result, is(nullValue()));
					} catch (IOException e1) {}
					return null;
				});
			}
		} finally {
			context.close();
		}
	}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories=true)
	@EnableJpaStores
	public static class TestConfig {

		@Bean
		public DbService dbService() {
			return new DbService();
		}
	}

	@Component
	public static class DbService {

		@Transactional
		public TestEntity doSomeDbStuff(TestEntityContentRepository store, TestEntity te) throws Exception {
			te = store.setContent(te, new ByteArrayInputStream("Spring Content World!".getBytes()));
			throw new RuntimeException("badness");
		}
	}

	@Entity
	@Getter
	@Setter
	@NoArgsConstructor
	@Table(name="test_entities")
	public class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, String> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
