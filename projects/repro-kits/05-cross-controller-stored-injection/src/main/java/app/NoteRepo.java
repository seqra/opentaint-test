package app;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NoteRepo extends JpaRepository<Note, Long> { Note findByUrlIsNotNull(); }
