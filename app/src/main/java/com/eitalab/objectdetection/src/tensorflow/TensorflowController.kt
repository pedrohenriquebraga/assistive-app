package com.eitalab.objectdetection.src.tensorflow

import android.graphics.Bitmap
import android.util.Log
import com.eitalab.objectdetection.ui.DetectionResult
import org.tensorflow.lite.InterpreterApi as TfInterpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.DataType
import java.io.File
import kotlin.math.exp

class TensorflowController(private var modelFile: File) {

    private lateinit var interpreter: TfInterpreter
    private var started = false
    private var inputSize = 224
    private var numClasses = 1000

    private val imagenetClasses = mapOf(
        0 to "tench", 1 to "goldfish", 2 to "shark", 3 to "shark", 4 to "hammerhead",
        5 to "ray", 6 to "stingray", 7 to "cock", 8 to "hen", 9 to "ostrich",
        10 to "brambling", 11 to "goldfinch", 12 to "finch", 13 to "junco", 14 to "bunting",
        15 to "robin", 16 to "bulbul", 17 to "jay", 18 to "magpie", 19 to "chickadee",
        20 to "dipper", 21 to "kite", 22 to "eagle", 23 to "vulture", 24 to "owl",
        25 to "salamander", 26 to "newt", 27 to "eft", 28 to "salamander", 29 to "axolotl",
        30 to "bullfrog", 31 to "frog", 32 to "toad", 33 to "turtle", 34 to "turtle",
        35 to "turtle", 36 to "terrapin", 37 to "turtle", 38 to "gecko", 39 to "iguana",
        40 to "anole", 41 to "lizard", 42 to "agama", 43 to "lizard", 44 to "lizard",
        45 to "monster", 46 to "lizard", 47 to "chameleon", 48 to "dragon", 49 to "crocodile",
        50 to "alligator", 51 to "triceratops", 52 to "snake", 53 to "snake", 54 to "snake",
        55 to "snake", 56 to "snake", 57 to "snake", 58 to "snake", 59 to "snake",
        60 to "snake", 61 to "constrictor", 62 to "python", 63 to "cobra", 64 to "mamba",
        65 to "snake", 66 to "viper", 67 to "diamondback", 68 to "sidewinder", 69 to "trilobite",
        70 to "harvestman", 71 to "scorpion", 72 to "spider", 73 to "spider", 74 to "spider",
        75 to "widow", 76 to "tarantula", 77 to "spider", 78 to "tick", 79 to "centipede",
        80 to "grouse", 81 to "ptarmigan", 82 to "partridge", 83 to "chicken", 84 to "peacock",
        85 to "quail", 86 to "partridge", 87 to "grey", 88 to "macaw", 89 to "cockatoo",
        90 to "lorikeet", 91 to "coucal", 92 to "eater", 93 to "hornbill", 94 to "hummingbird",
        95 to "jacamar", 96 to "toucan", 97 to "drake", 98 to "merganser", 99 to "goose",
        100 to "swan", 101 to "tusker", 102 to "echidna", 103 to "platypus", 104 to "wallaby",
        105 to "koala", 106 to "wombat", 107 to "jellyfish", 108 to "anemone", 109 to "coral",
        110 to "flatworm", 111 to "roundworm", 112 to "conch", 113 to "snail", 114 to "slug",
        115 to "slug", 116 to "chiton", 117 to "nautilus", 118 to "crab", 119 to "crab",
        120 to "crab", 121 to "crab", 122 to "lobster", 123 to "lobster", 124 to "crayfish",
        125 to "crab", 126 to "isopod", 127 to "stork", 128 to "stork", 129 to "spoonbill",
        130 to "flamingo", 131 to "heron", 132 to "egret", 133 to "bittern", 134 to "crane",
        135 to "limpkin", 136 to "gallinule", 137 to "coot", 138 to "bustard", 139 to "turnstone",
        140 to "dunlin", 141 to "redshank", 142 to "dowitcher", 143 to "oysterWacher", 144 to "pelican",
        145 to "penguin", 146 to "albatross", 147 to "whale", 148 to "whale", 149 to "dugong",
        150 to "lion", 151 to "Chihuahua", 152 to "spaniel", 153 to "Maltese", 154 to "Pekinese",
        155 to "Shih-Tzu", 156 to "spaniel", 157 to "papillon", 158 to "terrier", 159 to "ridgeback",
        160 to "hound", 161 to "hound", 162 to "beagle", 163 to "bloodhound", 164 to "bluetick",
        165 to "coonhound", 166 to "hound", 167 to "foxhound", 168 to "redbone", 169 to "borzoi",
        170 to "wolfhound", 171 to "greyhound", 172 to "whippet", 173 to "hound", 174 to "elkhound",
        175 to "otterhound", 176 to "Saluki", 177 to "deerhound", 178 to "Weimaraner", 179 to "bullterrier",
        180 to "terrier", 181 to "terrier", 182 to "terrier", 183 to "terrier", 184 to "terrier",
        185 to "terrier", 186 to "terrier", 187 to "terrier", 188 to "terrier", 189 to "terrier",
        190 to "Sealyham", 191 to "Airedale", 192 to "cairn", 193 to "terrier", 194 to "Dinmont",
        195 to "terrier", 196 to "schnauzer", 197 to "schnauzer", 198 to "schnauzer", 199 to "Scottie",
        200 to "dog", 201 to "silky", 202 to "terrier", 203 to "terrier", 204 to "Lhasa",
        205 to "retriever", 206 to "retriever", 207 to "retriever", 208 to "retriever", 209 to "retriever",
        210 to "pointer", 211 to "vizsla", 212 to "setter", 213 to "setter", 214 to "setter",
        215 to "spaniel", 216 to "clumber", 217 to "springer", 218 to "spaniel", 219 to "cocker",
        220 to "spaniel", 221 to "spaniel", 222 to "kuvasz", 223 to "schipperke", 224 to "groenendael",
        225 to "malinois", 226 to "briard", 227 to "kelpie", 228 to "komondor", 229 to "bobtail",
        230 to "Shetland", 231 to "collie", 232 to "collie", 233 to "Bouvier", 234 to "Rottweiler",
        235 to "alsatian", 236 to "Doberman", 237 to "pinscher", 238 to "dog", 239 to "dog",
        240 to "Appenzeller", 241 to "EntleBucher", 242 to "boxer", 243 to "mastiff", 244 to "mastiff",
        245 to "bulldog", 246 to "Dane", 247 to "Bernard", 248 to "husky", 249 to "malamute",
        250 to "husky", 251 to "dalmatian", 252 to "affenpinscher", 253 to "basenji", 254 to "pug-dog",
        255 to "Leonberg", 256 to "Newfoundland", 257 to "Pyrenees", 258 to "Samoyed", 259 to "Pomeranian",
        260 to "chow", 261 to "keeshond", 262 to "griffon", 263 to "Pembroke", 264 to "Cardigan",
        265 to "poodle", 266 to "poodle", 267 to "poodle", 268 to "hairless", 269 to "wolf",
        270 to "wolf", 271 to "wolf", 272 to "coyote", 273 to "dingo", 274 to "dhole",
        275 to "dog", 276 to "hyaena", 277 to "fox", 278 to "fox", 279 to "fox",
        280 to "fox", 281 to "cat", 282 to "cat", 283 to "cat", 284 to "Siamese",
        285 to "cat", 286 to "concolor", 287 to "cat", 288 to "pardus", 289 to "uncia",
        290 to "onca", 291 to "leo", 292 to "tigris", 293 to "jubatus", 294 to "arctos",
        295 to "americanus", 296 to "maritimus", 297 to "ursinus", 298 to "mongoose", 299 to "meerkat",
        300 to "beetle", 301 to "beetle", 302 to "beetle", 303 to "beetle", 304 to "chrysomelid",
        305 to "beetle", 306 to "beetle", 307 to "weevil", 308 to "fly", 309 to "bee",
        310 to "pismire", 311 to "hopper", 312 to "cricket", 313 to "insect", 314 to "roach",
        315 to "mantid", 316 to "cicala", 317 to "leafhopper", 318 to "fly", 319 to "hawk",
        320 to "damselfly", 321 to "admiral", 322 to "butterfly", 323 to "plexippus", 324 to "butterfly",
        325 to "butterfly", 326 to "butterfly", 327 to "star", 328 to "urchin", 329 to "holothurian",
        330 to "rabbit", 331 to "hare", 332 to "rabbit", 333 to "hamster", 334 to "hedgehog",
        335 to "niger", 336 to "marmot", 337 to "beaver", 338 to "cobaya", 339 to "sorrel",
        340 to "zebra", 341 to "scrofa", 342 to "boar", 343 to "warthog", 344 to "amphibius",
        345 to "ox", 346 to "bubalis", 347 to "bison", 348 to "tup", 349 to "canadensis",
        350 to "ibex", 351 to "hartebeest", 352 to "melampus", 353 to "gazelle", 354 to "dromedarius",
        355 to "llama", 356 to "weasel", 357 to "mink", 358 to "putorius", 359 to "nigripes",
        360 to "otter", 361 to "pussy", 362 to "badger", 363 to "armadillo", 364 to "tridactylus",
        365 to "pygmaeus", 366 to "gorilla", 367 to "troglodytes", 368 to "lar", 369 to "syndactylus",
        370 to "monkey", 371 to "patas", 372 to "baboon", 373 to "macaque", 374 to "langur",
        375 to "monkey", 376 to "larvatus", 377 to "marmoset", 378 to "capucinus", 379 to "howler",
        380 to "monkey", 381 to "geoffroyi", 382 to "sciureus", 383 to "catta", 384 to "brevicaudatus",
        385 to "maximus", 386 to "africana", 387 to "fulgens", 388 to "melanoleuca", 389 to "snoek",
        390 to "eel", 391 to "kisutch", 392 to "tricolor", 393 to "fish", 394 to "sturgeon",
        395 to "osseus", 396 to "lionfish", 397 to "globefish", 398 to "abacus", 399 to "abaya",
        400 to "robe", 401 to "box", 402 to "guitar", 403 to "carrier", 404 to "airliner",
        405 to "dirigible", 406 to "altar", 407 to "ambulance", 408 to "vehicle", 409 to "clock",
        410 to "house", 411 to "apron", 412 to "dustbin", 413 to "gun", 414 to "haversack",
        415 to "bakehouse", 416 to "beam", 417 to "balloon", 418 to "Biro", 419 to "Aid",
        420 to "banjo", 421 to "handrail", 422 to "barbell", 423 to "chair", 424 to "barbershop",
        425 to "barn", 426 to "barometer", 427 to "cask", 428 to "wheelbarrow", 429 to "baseball",
        430 to "basketball", 431 to "bassinet", 432 to "bassoon", 433 to "cap", 434 to "towel",
        435 to "tub", 436 to "waggon", 437 to "pharos", 438 to "beaker", 439 to "shako",
        440 to "bottle", 441 to "glass", 442 to "cot", 443 to "bib", 444 to "tandem",
        445 to "two-piece", 446 to "ring-binder", 447 to "glasses", 448 to "birdhouse", 449 to "boathouse",
        450 to "bob", 451 to "bola", 452 to "bonnet", 453 to "bookcase", 454 to "bookstall",
        455 to "bottlecap", 456 to "bow", 457 to "bowtie", 458 to "plaque", 459 to "bandeau",
        460 to "jetty", 461 to "egis", 462 to "broom", 463 to "pail", 464 to "buckle",
        465 to "vest", 466 to "train", 467 to "market", 468 to "taxicab", 469 to "cauldron",
        470 to "light", 471 to "cannon", 472 to "canoe", 473 to "opener", 474 to "cardigan",
        475 to "mirror", 476 to "whirligig", 477 to "kit", 478 to "carton", 479 to "wheel",
        480 to "ATM", 481 to "cassette", 482 to "player", 483 to "castle", 484 to "catamaran",
        485 to "player", 486 to "violoncello", 487 to "phone", 488 to "chain", 489 to "fence",
        490 to "armour", 491 to "chainsaw", 492 to "chest", 493 to "commode", 494 to "gong",
        495 to "closet", 496 to "stocking", 497 to "building", 498 to "palace", 499 to "chopper",
        500 to "dwelling", 501 to "cloak", 502 to "sabot", 503 to "shaker", 504 to "mug",
        505 to "coffeepot", 506 to "helix", 507 to "lock", 508 to "keypad", 509 to "store",
        510 to "vessel", 511 to "convertible", 512 to "screw", 513 to "trump", 514 to "boot",
        515 to "hat", 516 to "cradle", 517 to "crane", 518 to "helmet", 519 to "crate",
        520 to "cot", 521 to "Pot", 522 to "ball", 523 to "crutch", 524 to "cuirass",
        525 to "dyke", 526 to "desk", 527 to "computer", 528 to "phone", 529 to "napkin",
        530 to "clock", 531 to "watch", 532 to "board", 533 to "dishcloth", 534 to "machine",
        535 to "brake", 536 to "facility", 537 to "sleigh", 538 to "dome", 539 to "mat",
        540 to "rig", 541 to "tympan", 542 to "drumstick", 543 to "dumbbell", 544 to "oven",
        545 to "blower", 546 to "guitar", 547 to "locomotive", 548 to "center", 549 to "envelope",
        550 to "maker", 551 to "powder", 552 to "boa", 553 to "cabinet", 554 to "fireboat",
        555 to "truck", 556 to "fireguard", 557 to "flagstaff", 558 to "flute", 559 to "chair",
        560 to "helmet", 561 to "forklift", 562 to "fountain", 563 to "pen", 564 to "four-poster",
        565 to "car", 566 to "horn", 567 to "skillet", 568 to "coat", 569 to "dustcart",
        570 to "helmet", 571 to "dispenser", 572 to "goblet", 573 to "go-kart", 574 to "ball",
        575 to "cart", 576 to "gondola", 577 to "tam-tam", 578 to "gown", 579 to "grand",
        580 to "glasshouse", 581 to "grille", 582 to "market", 583 to "guillotine", 584 to "slide",
        585 to "spray", 586 to "track", 587 to "hammer", 588 to "hamper", 589 to "drier",
        590 to "microcomputer", 591 to "hankey", 592 to "disk", 593 to "harp", 594 to "harp",
        595 to "reaper", 596 to "hatchet", 597 to "holster", 598 to "theatre", 599 to "honeycomb",
        600 to "claw", 601 to "crinoline", 602 to "bar", 603 to "horse-cart", 604 to "hourglass",
        605 to "iPod", 606 to "iron", 607 to "jack-o'-lantern", 608 to "denim", 609 to "landrover",
        610 to "shirt", 611 to "puzzle", 612 to "rickshaw", 613 to "joystick", 614 to "kimono",
        615 to "pad", 616 to "knot", 617 to "coat", 618 to "ladle", 619 to "shade",
        620 to "computer", 621 to "mower", 622 to "cover", 623 to "paperknife", 624 to "library",
        625 to "lifeboat", 626 to "ignitor", 627 to "limo", 628 to "liner", 629 to "rouge",
        630 to "Loafer", 631 to "lotion", 632 to "system", 633 to "loupe", 634 to "sawmill",
        635 to "compass", 636 to "postbag", 637 to "box", 638 to "maillot", 639 to "suit",
        640 to "cover", 641 to "maraca", 642 to "xylophone", 643 to "mask", 644 to "matchstick",
        645 to "maypole", 646 to "labyrinth", 647 to "cup", 648 to "cabinet", 649 to "structure",
        650 to "mike", 651 to "oven", 652 to "uniform", 653 to "can", 654 to "minibus",
        655 to "mini", 656 to "minivan", 657 to "missile", 658 to "mitten", 659 to "bowl",
        660 to "home", 661 to "T", 662 to "modem", 663 to "monastery", 664 to "monitor",
        665 to "moped", 666 to "mortar", 667 to "mortarboard", 668 to "mosque", 669 to "net",
        670 to "scooter", 671 to "off-roader", 672 to "tent", 673 to "mouse", 674 to "mousetrap",
        675 to "van", 676 to "muzzle", 677 to "nail", 678 to "brace", 679 to "necklace",
        680 to "nipple", 681 to "computer", 682 to "obelisk", 683 to "hautbois", 684 to "potato",
        685 to "milometer", 686 to "filter", 687 to "organ", 688 to "CRO", 689 to "overskirt",
        690 to "oxcart", 691 to "mask", 692 to "packet", 693 to "paddle", 694 to "wheel",
        695 to "padlock", 696 to "paintbrush", 697 to "jammies", 698 to "palace", 699 to "syrinx",
        700 to "towel", 701 to "chute", 702 to "bars", 703 to "bench", 704 to "meter",
        705 to "carriage", 706 to "terrace", 707 to "pay-station", 708 to "footstall", 709 to "case",
        710 to "sharpener", 711 to "essence", 712 to "dish", 713 to "photocopier", 714 to "plectron",
        715 to "pickelhaube", 716 to "paling", 717 to "truck", 718 to "pier", 719 to "bank",
        720 to "bottle", 721 to "pillow", 722 to "ball", 723 to "pinwheel", 724 to "ship",
        725 to "ewer", 726 to "plane", 727 to "planetarium", 728 to "bag", 729 to "rack",
        730 to "plough", 731 to "helper", 732 to "camera", 733 to "pole", 734 to "Maria",
        735 to "poncho", 736 to "table", 737 to "bottle", 738 to "flowerpot", 739 to "wheel",
        740 to "drill", 741 to "mat", 742 to "printer", 743 to "house", 744 to "missile",
        745 to "projector", 746 to "puck", 747 to "punchball", 748 to "purse", 749 to "pen",
        750 to "puff", 751 to "car", 752 to "racquet", 753 to "radiator", 754 to "wireless",
        755 to "reflector", 756 to "barrel", 757 to "R.V.", 758 to "reel", 759 to "camera",
        760 to "icebox", 761 to "remote", 762 to "eatery", 763 to "six-shooter", 764 to "rifle",
        765 to "rocker", 766 to "rotisserie", 767 to "eraser", 768 to "ball", 769 to "ruler",
        770 to "shoe", 771 to "safe", 772 to "pin", 773 to "shaker", 774 to "sandal",
        775 to "sarong", 776 to "saxophone", 777 to "scabbard", 778 to "machine", 779 to "bus",
        780 to "schooner", 781 to "scoreboard", 782 to "screen", 783 to "screw", 784 to "screwdriver",
        785 to "seatbelt", 786 to "machine", 787 to "buckler", 788 to "store", 789 to "shoji",
        790 to "basket", 791 to "cart", 792 to "shovel", 793 to "cap", 794 to "curtain",
        795 to "ski", 796 to "mask", 797 to "bag", 798 to "slipstick", 799 to "door",
        800 to "bandit", 801 to "snorkel", 802 to "snowmobile", 803 to "snowplough", 804 to "dispenser",
        805 to "ball", 806 to "sock", 807 to "furnace", 808 to "sombrero", 809 to "bowl",
        810 to "bar", 811 to "heater", 812 to "shuttle", 813 to "spatula", 814 to "speedboat",
        815 to "web", 816 to "spindle", 817 to "car", 818 to "spot", 819 to "stage",
        820 to "locomotive", 821 to "bridge", 822 to "drum", 823 to "stethoscope", 824 to "stole",
        825 to "wall", 826 to "watch", 827 to "stove", 828 to "strainer", 829 to "car",
        830 to "stretcher", 831 to "bed", 832 to "tope", 833 to "U-boat", 834 to "clothes",
        835 to "sundial", 836 to "sunglass", 837 to "shades", 838 to "blocker", 839 to "bridge",
        840 to "mop", 841 to "sweatshirt", 842 to "trunks", 843 to "swing", 844 to "switch",
        845 to "syringe", 846 to "lamp", 847 to "vehicle", 848 to "player", 849 to "teapot",
        850 to "bear", 851 to "system", 852 to "ball", 853 to "roof", 854 to "curtain",
        855 to "thimble", 856 to "machine", 857 to "throne", 858 to "roof", 859 to "toaster",
        860 to "tobacconist", 861 to "seat", 862 to "torch", 863 to "pole", 864 to "wrecker",
        865 to "toyshop", 866 to "tractor", 867 to "semi", 868 to "tray", 869 to "coat",
        870 to "velocipede", 871 to "trimaran", 872 to "tripod", 873 to "arch", 874 to "trolley",
        875 to "trombone", 876 to "vat", 877 to "turnstile", 878 to "keyboard", 879 to "umbrella",
        880 to "monocycle", 881 to "piano", 882 to "cleaner", 883 to "vase", 884 to "vault",
        885 to "velvet", 886 to "machine", 887 to "vestment", 888 to "viaduct", 889 to "fiddle",
        890 to "volleyball", 891 to "iron", 892 to "clock", 893 to "pocketbook", 894 to "press",
        895 to "plane", 896 to "basin", 897 to "machine", 898 to "bottle", 899 to "jug",
        900 to "tower", 901 to "jug", 902 to "whistle", 903 to "wig", 904 to "screen",
        905 to "shade", 906 to "tie", 907 to "bottle", 908 to "wing", 909 to "wok",
        910 to "spoon", 911 to "woollen", 912 to "fence", 913 to "wreck", 914 to "yawl",
        915 to "yurt", 916 to "site", 917 to "book", 918 to "crossword", 919 to "sign",
        920 to "stoplight", 921 to "wrapper", 922 to "menu", 923 to "plate", 924 to "guacamole",
        925 to "consomme", 926 to "hotpot", 927 to "trifle", 928 to "icecream", 929 to "popsicle",
        930 to "loaf", 931 to "beigel", 932 to "pretzel", 933 to "cheeseburger", 934 to "hot",
        935 to "potato", 936 to "cabbage", 937 to "broccoli", 938 to "cauliflower", 939 to "courgette",
        940 to "squash", 941 to "squash", 942 to "squash", 943 to "cuke", 944 to "artichoke",
        945 to "pepper", 946 to "cardoon", 947 to "mushroom", 948 to "Smith", 949 to "strawberry",
        950 to "orange", 951 to "lemon", 952 to "fig", 953 to "ananas", 954 to "banana",
        955 to "jack", 956 to "apple", 957 to "pomegranate", 958 to "hay", 959 to "carbonara",
        960 to "syrup", 961 to "dough", 962 to "meatloaf", 963 to "pizza", 964 to "potpie",
        965 to "burrito", 966 to "wine", 967 to "espresso", 968 to "cup", 969 to "eggnog",
        970 to "alp", 971 to "bubble", 972 to "drop-off", 973 to "reef", 974 to "geyser",
        975 to "lakeshore", 976 to "foreland", 977 to "bar", 978 to "sea-coast", 979 to "vale",
        980 to "volcano", 981 to "player", 982 to "bridegroom", 983 to "diver", 984 to "rapeseed",
        985 to "daisy", 986 to "calceolus", 987 to "corn", 988 to "acorn", 989 to "rosehip",
        990 to "conker", 991 to "fungus", 992 to "agaric", 993 to "gyromitra", 994 to "fungus",
        995 to "earthstar", 996 to "frondosa", 997 to "bolete", 998 to "capitulum", 999 to "tissue"
    )

    private val minConf = 0.05f

    init {
        initTensorflow()
    }

    private fun initTensorflow() {
        val options = TfInterpreter.Options().apply {
            setRuntime(TfInterpreter.Options.TfLiteRuntime.PREFER_SYSTEM_OVER_APPLICATION)
            setNumThreads(4)
        }
        try {
            interpreter = TfInterpreter.create(modelFile, options)
            started = true

            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            if (inputShape.size >= 2) {
                inputSize = inputShape[1]
            }

            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            if (outputShape.size >= 2) {
                numClasses = outputShape[1]
            }

            Log.i("TensorflowController", "Modelo ConvNeXt Base carregado com sucesso. Input shape: ${inputShape.contentToString()}, Output shape: ${outputShape.contentToString()}")
        } catch (e: Exception) {
            Log.e("TensorflowController", "Erro ao iniciar Tensorflow: ${e.message}")
        }
    }

    fun detect(imgInput: Bitmap): MutableList<DetectionResult>? {
        if (!started) return null

        val tensorImage = processImage(imgInput)
        val outputProbability = Array(1) { FloatArray(numClasses) }

        try {
            interpreter.run(tensorImage.buffer, outputProbability)
        } catch (e: Exception) {
            Log.e("TensorflowController", "Erro na inferência: ${e.message}")
            return null
        }

        val rawScores = outputProbability[0]
        val probabilities = softmax(rawScores, 0.25f) // Temperatura ajustada para garantir probabilidades altas e nítidas

        val detections = mutableListOf<DetectionResult>()

        for (i in probabilities.indices) {
            val score = probabilities[i]
            if (score >= minConf) {
                val label = imagenetClasses[i] ?: "Classe $i"
                detections.add(DetectionResult(label, score))
            }
        }

        detections.sortByDescending { data -> data.confidence }

        return if (detections.isNotEmpty()) {
            mutableListOf(detections.first())
        } else {
            mutableListOf()
        }
    }

    private fun softmax(logits: FloatArray, temperature: Float = 0.25f): FloatArray {
        var max = logits[0]
        for (i in 1 until logits.size) {
            if (logits[i] > max) max = logits[i]
        }
        var sum = 0f
        val exp = FloatArray(logits.size)
        for (i in logits.indices) {
            exp[i] = exp(((logits[i] - max) / temperature).toDouble()).toFloat()
            sum += exp[i]
        }
        if (sum == 0f) sum = 1f
        for (i in exp.indices) {
            exp[i] /= sum
        }
        return exp
    }

    private fun processImage(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        return imageProcessor.process(tensorImage)
    }

    fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
