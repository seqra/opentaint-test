package app;
import jakarta.persistence.*;
@Entity public class Note2 { @Id public Long id; public String url;
    public Note2() {} public Note2(String url){ this.url = url; }
    public String getUrl(){ return url; } }
