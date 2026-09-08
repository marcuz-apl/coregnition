package io.github.marcuzapl.coregnition.backend.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

@Configuration
public class DatabaseConfiguration {
    @Bean
    DataSource dataSource(@Value("${coregnition.database:data/coregnition.db}") String database) throws Exception {
        Path path = Path.of(database).toAbsolutePath().normalize();
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + path);
        return source;
    }
}
