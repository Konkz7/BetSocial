package com.example.World.Threads;


import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends ListCrudRepository<Thread_, Long> {


    @Modifying
    @Transactional
    @Query("UPDATE Thread_ SET  deleted_at = :deleted_at WHERE tid = :id AND deleted_at IS NULL")
    int remove(
            @Param("id") Long id,
            @Param("deleted_at") Long deleted_at);


/*
    List<Thread> findAll();

    Optional<Thread> findByTid(Integer tid);

    void create(Thread thread);

    void update( Thread thread, Integer tid);

    void delete(Integer tid);

    int count();

    void saveAll(List<Thread> threads);

 */


}
