package jl.uz.graphql;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SpringBootApplication
public class GraphqlApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphqlApplication.class, args);
    }

}


@Controller
class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /*@SchemaMapping(typeName = "Query")*/
    @QueryMapping
    public List<Book> allBooks() {
        return bookRepository.findAll();
    }

    @QueryMapping
    public Book findOne(@Argument Integer id) {
        return bookRepository.findById(id).orElse(null);
    }
}


@Controller
class AuthorController {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public AuthorController(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @QueryMapping
    public List<Author> allAuthors() {
        List<Author> authors = authorRepository.findAll();
        for (Author author : authors) {
            author.setBooks(bookRepository.findAllByAuthor_Id(author.getId()));
        }
        return authors;
    }

    //@SchemaMapping(typeName = "Mutation", value = "updateAuthor")
    @MutationMapping
    public Author updateAuthor(@Argument AuthorUpdateDTO dto) {
        Author author = authorRepository.findById(dto.getId()).orElseThrow(() -> {
            throw new RuntimeException("Not  found");
        });
        if (Objects.nonNull(dto.getFirstName()))
            author.setFirstName(dto.getFirstName());
        if (Objects.nonNull(dto.getLastName()))
            author.setLastName(dto.getLastName());
        authorRepository.save(author);
        return author;
    }

}

interface BookRepository extends JpaRepository<Book, Integer> {
    List<Book> findAllByAuthor_Id(Integer id);
}


interface AuthorRepository extends JpaRepository<Author, Integer> {
}


@Component
class Init implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AuthorRepository authorRepository = applicationContext.getBean(AuthorRepository.class);
        BookRepository bookRepository = applicationContext.getBean(BookRepository.class);

        authorRepository.deleteAll();
        bookRepository.deleteAll();

        Author josh = new Author(1, "Josh", "Long");
        Author mark = new Author(2, "Mark", "Heckler");
        Author greg = new Author(3, "Greg", "Turnquist");
        authorRepository.saveAllAndFlush(Arrays.asList(josh, mark, greg));

        bookRepository.saveAll(
                List.of(
                        new Book(1, "Reactive Spring", 484, Rating.FIVE_STARS, josh),
                        new Book(2, "Spring Boot Up & Running", 328, Rating.FIVE_STARS, mark),
                        new Book(3, "Hacking with Spring Boot 2.3", 392, Rating.FIVE_STARS, greg)
                )
        );

    }
}

enum Rating {
    FIVE_STARS("⭐️⭐️⭐️⭐️⭐️️️️", "5"),
    FOUR_STARS("⭐️⭐️⭐️⭐️", "4"),
    THREE_STARS("⭐️⭐️⭐️", "3"),
    TWO_STARS("⭐️⭐️", "2"),
    ONE_STAR("⭐️", "1");

    private String star;
    private String rating;

    Rating(String star, String rating) {
        this.star = star;
        this.rating = rating;
    }

    @JsonValue
    public String getStar() {
        return star;
    }
}


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
class Book {
    @Id
    private Integer id;
    private String title;
    private Integer pages;
    private Rating rating;

    @ManyToOne(targetEntity = Author.class, fetch = FetchType.LAZY)
    private Author author;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
class Product {
    @Id
    private Integer id;
    private String name;
    private String description;
    private Size size;
}

interface ProductRepo extends JpaRepository<Product, Integer> {
}

enum Size {
    S, M, L, XL, XXL
}


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "book_auhor")
class Author {
    @Id
    private Integer id;
    private String firstName;
    private String lastName;

    @OneToMany
    private List<Book> books;

    public Author(int id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }


    public String fullName() {
        return firstName + " " + lastName;
    }

}


@Data
@NoArgsConstructor
@AllArgsConstructor
class AuthorUpdateDTO {
    private Integer id;
    private String firstName;
    private String lastName;
}