package com.maxpilotto.kon.samples;

import com.maxpilotto.kon.samples.parsing.Author;
import com.maxpilotto.kon.samples.parsing.AuthorKt;

public class ObjectEncoding {
    public static void main(String[] args) {
        Author author = new Author("George","Orwell",1903);

        System.out.println(AuthorKt.encode(author));
    }
}
