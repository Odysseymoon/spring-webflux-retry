package moon.odyssey.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    private Integer id;

    private String title;

    private String body;

    private Integer userId;
}
