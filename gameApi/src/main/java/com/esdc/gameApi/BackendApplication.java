package com.esdc.gameApi;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
public class BackendApplication implements CommandLineRunner {

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("=== Проверка подключения к базе ===");
		System.out.println("Connected to: " + dataSource.getConnection().getMetaData().getURL());

		System.out.println("\n=== USERS_ACHIEVEMENTS ===");
		List<Map<String, Object>> uaRows = jdbcTemplate.queryForList("SELECT * FROM users_achievements");
		if (uaRows.isEmpty()) {
			System.out.println("Нет данных в users_achievements");
		} else {
			uaRows.forEach(System.out::println);
		}

		System.out.println("\n=== ACHIEVEMENTS ===");
		List<Map<String, Object>> aRows = jdbcTemplate.queryForList("SELECT * FROM achievements");
		if (aRows.isEmpty()) {
			System.out.println("Нет данных в achievements");
		} else {
			aRows.forEach(System.out::println);
		}

		System.out.println("\n=== LEVELS ===");
		List<Map<String, Object>> lRows = jdbcTemplate.queryForList("SELECT * FROM levels");
		if (lRows.isEmpty()) {
			System.out.println("Нет данных в levels");
		} else {
			lRows.forEach(System.out::println);
		}

		System.out.println("Datasource URL = " +
				System.getProperty("DB_HOST") + ":" +
				System.getProperty("DB_PORT") + "/" +
				System.getProperty("POSTGRES_DB"));

	}
}

