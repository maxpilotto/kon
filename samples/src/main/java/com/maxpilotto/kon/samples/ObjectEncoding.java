package com.maxpilotto.kon.samples;

import com.maxpilotto.kon.samples.encoding.Author;
import com.maxpilotto.kon.samples.encoding.AuthorKt;

public class ObjectEncoding {
    public static void main(String[] args) {
        Author author = new Author("George","Orwell",1903);

        System.out.println(AuthorKt.encode(author));
    }
}
