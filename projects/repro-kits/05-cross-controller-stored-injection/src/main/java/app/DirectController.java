package app;
import java.net.URL;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/direct")
public class DirectController {
    @PostMapping("/body") public void body(@RequestBody String url) throws Exception { new URL(url).openStream(); }
}
