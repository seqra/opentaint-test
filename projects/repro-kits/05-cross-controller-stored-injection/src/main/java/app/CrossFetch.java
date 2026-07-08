package app;
import java.net.URL;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/cross-fetch")
public class CrossFetch {
    private final Note2Repo repo;
    public CrossFetch(Note2Repo repo){ this.repo = repo; }
    @GetMapping public void fetch() throws Exception { new URL(repo.findByUrlIsNotNull().getUrl()).openStream(); }
}
