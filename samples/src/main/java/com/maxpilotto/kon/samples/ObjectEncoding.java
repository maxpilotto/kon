package com.maxpilotto.kon.samples;

import com.maxpilotto.kon.samples.encoding.Author;
import com.maxpilotto.kon.samples.encoding.AuthorEncoder;

public class ObjectEncoding {
    public static void main(String[] args) {
        Author author = new Author("George","Orwell",1903);

        System.out.println(AuthorEncoder.encode(author));
    }
}
