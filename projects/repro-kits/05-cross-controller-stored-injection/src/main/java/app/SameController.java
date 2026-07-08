package app;
import java.net.URL;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/same")
public class SameController {
    private final NoteRepo repo;
    public SameController(NoteRepo repo){ this.repo = repo; }
    @PostMapping("/store") public void store(@RequestBody String url){ repo.save(new Note(url)); }
    @GetMapping("/fetch") public void fetch() throws Exception { new URL(repo.findByUrlIsNotNull().getUrl()).openStream(); }
}
