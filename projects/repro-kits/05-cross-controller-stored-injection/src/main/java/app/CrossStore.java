package app;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/cross-store")
public class CrossStore {
    private final Note2Repo repo;
    public CrossStore(Note2Repo repo){ this.repo = repo; }
    @PostMapping public void store(@RequestBody String url){ repo.save(new Note2(url)); }
}
