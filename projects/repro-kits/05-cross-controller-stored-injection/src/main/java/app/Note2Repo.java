package app;
import org.springframework.data.jpa.repository.JpaRepository;
public interface Note2Repo extends JpaRepository<Note2, Long> { Note2 findByUrlIsNotNull(); }
