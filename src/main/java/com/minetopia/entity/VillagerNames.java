package com.minetopia.entity;

import net.minecraft.util.RandomSource;

import java.util.List;

/** Pool of first names assigned randomly to villagers on first spawn. */
public final class VillagerNames {

    private VillagerNames() {}

    private static final List<String> MALE = List.of(
            "John", "James", "William", "Thomas", "George", "Henry", "Arthur",
            "Walter", "Harold", "Albert", "Ernest", "Frank", "Alfred", "Frederick",
            "Samuel", "Barnaby", "Cedric", "Edmund", "Gilbert", "Herbert", "Jasper",
            "Leonard", "Maurice", "Nigel", "Oliver", "Percy", "Reginald", "Sidney",
            "Theodore", "Victor", "Wilbur", "Amos", "Crispin", "Dorian", "Elias",
            "Fletcher", "Garrett", "Hadwin", "Ingram", "Jorah", "Keiran", "Leofric",
            "Milo", "Norton", "Oswin", "Piers", "Quentin", "Roderick", "Selwyn"
    );

    private static final List<String> FEMALE = List.of(
            "Mary", "Emma", "Alice", "Florence", "Edith", "Elsie", "Doris", "Dorothy",
            "Ethel", "Gertrude", "Grace", "Hannah", "Irene", "Joan", "Katherine",
            "Laura", "Margaret", "Nancy", "Olive", "Phyllis", "Rose", "Sarah",
            "Agnes", "Beatrice", "Cecily", "Eleanor", "Frances", "Harriet", "Ida",
            "Jane", "Lillian", "Mabel", "Nora", "Penelope", "Ruth", "Sylvia",
            "Violet", "Winifred", "Constance", "Millicent", "Aveline", "Briar",
            "Cordelia", "Damaris", "Evelyn", "Rowena", "Seraphina", "Thessaly"
    );

    public static String pick(boolean male, RandomSource rng) {
        List<String> pool = male ? MALE : FEMALE;
        return pool.get(rng.nextInt(pool.size()));
    }
}
