package com.tangem.crypto.bip39

object MnemonicTestData {

    const val MNEMONIC_12_WORDS = "model kit endorse oil eagle twist web betray library suit beyond reward"
    const val EXPECTED_12_WORDS_SEED =
        "fb92478c2742eb7e4d128e990cb3c4c1dd0f4e127caa3ad5ef97ef9b0dd16ad72c61133b9ce06be1d" +
            "35af97b69b4ae548db38a18a301be1af3d10637278e7d67"
    val mnemonic12ParsedComponents = listOf(
        "model", "kit", "endorse",
        "oil", "eagle", "twist",
        "web", "betray", "library",
        "suit", "beyond", "reward",
    )

    const val MNEMONIC_15_WORDS =
        "output any fruit odor eyebrow kiwi cliff acquire whisper snow vanish orange fade engine caution"
    const val EXPECTED_15_WORDS_SEED =
        "ed0d80eaf691bb69d4be30e9766ace4afb390814ccc93529c12a839bf8f28857" +
            "ecae94ef44e7d20722947af899269fbf4a3355b1f1b6641f3471d5baa1307480"
    val mnemonic15ParsedComponents = listOf(
        "output", "any", "fruit",
        "odor", "eyebrow", "kiwi",
        "cliff", "acquire", "whisper",
        "snow", "vanish", "orange",
        "fade", "engine", "caution",
    )

    const val MNEMONIC_18_WORDS =
        "hamster neutral similar ancient mixed cream tackle luggage cute" +
            " patient buzz fantasy push possible submit celery all wet"
    const val EXPECTED_18_WORDS_SEED =
        "e007a679d6e950e0e197a19ff6d1ed616eb6dbfada11d697bffa7bba556f8" +
            "7530ff297c09fb49442e6d6f731901e731987064bc52f8eb6ba326f68866020e5ed"
    val mnemonic18ParsedComponents = listOf(
        "hamster", "neutral", "similar",
        "ancient", "mixed", "cream",
        "tackle", "luggage", "cute",
        "patient", "buzz", "fantasy",
        "push", "possible", "submit",
        "celery", "all", "wet",
    )

    const val MNEMONIC_21_WORDS =
        "fun asset turkey insane grow outside marble shrug struggle flip" +
            " dumb depart neither pitch aerobic tiger margin crouch skill chunk nothing"
    const val EXPECTED_21_WORDS_SEED =
        "6127732357c2eca5fd322419b14e3a727d64758583e7a80a408d318cb719b" +
            "24d165edceff2a1855d7a914fa4f8b617b4c968e992f01ae4ebc759a1881bcd5cf4"
    val mnemonic21ParsedComponents = listOf(
        "fun", "asset", "turkey",
        "insane", "grow", "outside",
        "marble", "shrug", "struggle",
        "flip", "dumb", "depart",
        "neither", "pitch", "aerobic",
        "tiger", "margin", "crouch",
        "skill", "chunk", "nothing",
    )

    const val MNEMONIC_24_WORDS =
        "stand mandate skate rally noise bike inmate switch trigger turtle" +
            " until bracket stock lottery kite thing dove faculty strike capital swap dentist strike stove"
    const val EXPECTED_24_WORDS_SEED =
        "c9a8d4bd641bb1ddf130af8eb446d019265a0a6c22d35712eb4937885396a6ebc" +
            "34719e37f644de2d63d3805747d2647a659e336d9a82c216d75fceaf3b7fdf1"
    val mnemonic24ParsedComponents = listOf(
        "stand", "mandate", "skate",
        "rally", "noise", "bike",
        "inmate", "switch", "trigger",
        "turtle", "until", "bracket",
        "stock", "lottery", "kite",
        "thing", "dove", "faculty",
        "strike", "capital", "swap",
        "dentist", "strike", "stove",
    )
}