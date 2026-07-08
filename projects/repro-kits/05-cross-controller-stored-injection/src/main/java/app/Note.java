package app;
import jakarta.persistence.*;
@Entity public class Note { @Id public Long id; public String url;
    public Note() {} public Note(String url){ this.url = url; }
    public String getUrl(){ return url; } }
