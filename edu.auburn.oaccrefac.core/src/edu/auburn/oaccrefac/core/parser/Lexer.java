/*
State 0:
    0x9..0x9 => State 1
    0x20..0x20 => State 1
    !..! => State 21
    ".." => State 23
    #..# => State 39
    %..% => State 51
    &..& => State 52
    '..' => State 54
    (..( => State 69
    )..) => State 70
    *..* => State 71
    +..+ => State 72
    ,.., => State 74
    -..- => State 75
    .... => State 78
    /../ => State 85
    0..0 => State 86
    1..9 => State 133
    :..: => State 146
    <..< => State 147
    =..= => State 150
    >..> => State 152
    ?..? => State 155
    A..K => State 156
    L..L => State 164
    M..Z => State 156
    [..[ => State 165
    \..\ => State 166
    ]..] => State 172
    ^..^ => State 173
    _.._ => State 156
    a..a => State 174
    b..b => State 156
    c..c => State 179
    d..d => State 203
    e..e => State 156
    f..f => State 229
    g..g => State 241
    h..h => State 245
    i..i => State 254
    j..j => State 156
    k..k => State 266
    l..l => State 273
    m..m => State 277
    n..n => State 282
    o..o => State 156
    p..p => State 298
    q..q => State 156
    r..r => State 349
    s..s => State 358
    t..t => State 156
    u..u => State 366
    v..v => State 381
    w..w => State 394
    x..z => State 156
    |..| => State 403
    ~..~ => State 405

State 1 (FINAL - (skip)):
    0x9..0x9 => State 1 (Back Edge)
    0x20..0x20 => State 1 (Back Edge)
    /../ => State 2

State 2:
    *..* => State 3
    /../ => State 7

State 3:
    0x0..) => State 4
    *..* => State 5
    +..￿ => State 4

State 4:
    0x0..) => State 4 (Back Edge)
    *..* => State 5
    +..￿ => State 4 (Back Edge)

State 5:
    0x0..) => State 4 (Back Edge)
    *..* => State 5 (Back Edge)
    +... => State 4 (Back Edge)
    /../ => State 6
    0..￿ => State 4 (Back Edge)

State 6 (FINAL - (skip)):
    0x9..0x9 => State 1 (Back Edge)
    0x20..0x20 => State 1 (Back Edge)
    /../ => State 2 (Back Edge)

State 7 (FINAL - (skip)):
    0x0..0x8 => State 8
    0x9..0x9 => State 9
    0xb..0xc => State 8
    0xe..0x1f => State 8
    0x20..0x20 => State 9
    !... => State 8
    /../ => State 10
    0..￿ => State 8

State 8 (FINAL - (skip)):
    0x0..0x8 => State 8 (Back Edge)
    0x9..0x9 => State 9
    0xb..0xc => State 8 (Back Edge)
    0xe..0x1f => State 8 (Back Edge)
    0x20..0x20 => State 9
    !... => State 8 (Back Edge)
    /../ => State 10
    0..￿ => State 8 (Back Edge)

State 9 (FINAL - (skip)):
    0x0..0x8 => State 8 (Back Edge)
    0x9..0x9 => State 9 (Back Edge)
    0xb..0xc => State 8 (Back Edge)
    0xe..0x1f => State 8 (Back Edge)
    0x20..0x20 => State 9 (Back Edge)
    !... => State 8 (Back Edge)
    /../ => State 10
    0..￿ => State 8 (Back Edge)

State 10 (FINAL - (skip)):
    0x0..0x8 => State 8 (Back Edge)
    0x9..0x9 => State 9 (Back Edge)
    0xb..0xc => State 8 (Back Edge)
    0xe..0x1f => State 8 (Back Edge)
    0x20..0x20 => State 9 (Back Edge)
    !..) => State 8 (Back Edge)
    *..* => State 11
    +... => State 8 (Back Edge)
    /../ => State 16
    0..￿ => State 8 (Back Edge)

State 11 (FINAL - (skip)):
    0x0..0x8 => State 12
    0x9..0x9 => State 13
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12
    0x20..0x20 => State 13
    !..) => State 12
    *..* => State 14
    +... => State 12
    /../ => State 17
    0..￿ => State 12

State 12 (FINAL - (skip)):
    0x0..0x8 => State 12 (Back Edge)
    0x9..0x9 => State 13
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12 (Back Edge)
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12 (Back Edge)
    0x20..0x20 => State 13
    !..) => State 12 (Back Edge)
    *..* => State 14
    +... => State 12 (Back Edge)
    /../ => State 17
    0..￿ => State 12 (Back Edge)

State 13 (FINAL - (skip)):
    0x0..0x8 => State 12 (Back Edge)
    0x9..0x9 => State 13 (Back Edge)
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12 (Back Edge)
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12 (Back Edge)
    0x20..0x20 => State 13 (Back Edge)
    !..) => State 12 (Back Edge)
    *..* => State 14
    +... => State 12 (Back Edge)
    /../ => State 17
    0..￿ => State 12 (Back Edge)

State 14 (FINAL - (skip)):
    0x0..0x8 => State 12 (Back Edge)
    0x9..0x9 => State 13 (Back Edge)
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12 (Back Edge)
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12 (Back Edge)
    0x20..0x20 => State 13 (Back Edge)
    !..) => State 12 (Back Edge)
    *..* => State 14 (Back Edge)
    +... => State 12 (Back Edge)
    /../ => State 15
    0..￿ => State 12 (Back Edge)

State 15 (FINAL - (skip)):
    0x0..0x8 => State 8 (Back Edge)
    0x9..0x9 => State 9 (Back Edge)
    0xb..0xc => State 8 (Back Edge)
    0xe..0x1f => State 8 (Back Edge)
    0x20..0x20 => State 9 (Back Edge)
    !..) => State 8 (Back Edge)
    *..* => State 11 (Back Edge)
    +... => State 8 (Back Edge)
    /../ => State 16
    0..￿ => State 8 (Back Edge)

State 16 (FINAL - (skip)):
    0x0..0x8 => State 8 (Back Edge)
    0x9..0x9 => State 9 (Back Edge)
    0xb..0xc => State 8 (Back Edge)
    0xe..0x1f => State 8 (Back Edge)
    0x20..0x20 => State 9 (Back Edge)
    !..) => State 8 (Back Edge)
    *..* => State 11 (Back Edge)
    +... => State 8 (Back Edge)
    /../ => State 16 (Back Edge)
    0..￿ => State 8 (Back Edge)

State 17 (FINAL - (skip)):
    0x0..0x8 => State 12 (Back Edge)
    0x9..0x9 => State 13 (Back Edge)
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12 (Back Edge)
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12 (Back Edge)
    0x20..0x20 => State 13 (Back Edge)
    !..) => State 12 (Back Edge)
    *..* => State 18
    +... => State 12 (Back Edge)
    /../ => State 20
    0..￿ => State 12 (Back Edge)

State 18 (FINAL - (skip)):
    0x0..0x8 => State 12 (Back Edge)
    0x9..0x9 => State 13 (Back Edge)
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12 (Back Edge)
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12 (Back Edge)
    0x20..0x20 => State 13 (Back Edge)
    !..) => State 12 (Back Edge)
    *..* => State 14 (Back Edge)
    +... => State 12 (Back Edge)
    /../ => State 19
    0..￿ => State 12 (Back Edge)

State 19 (FINAL - (skip)):
    0x0..0x8 => State 12 (Back Edge)
    0x9..0x9 => State 13 (Back Edge)
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12 (Back Edge)
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12 (Back Edge)
    0x20..0x20 => State 13 (Back Edge)
    !..) => State 12 (Back Edge)
    *..* => State 18 (Back Edge)
    +... => State 12 (Back Edge)
    /../ => State 20
    0..￿ => State 12 (Back Edge)

State 20 (FINAL - (skip)):
    0x0..0x8 => State 12 (Back Edge)
    0x9..0x9 => State 13 (Back Edge)
    0xa..0xa => State 4 (Back Edge)
    0xb..0xc => State 12 (Back Edge)
    0xd..0xd => State 4 (Back Edge)
    0xe..0x1f => State 12 (Back Edge)
    0x20..0x20 => State 13 (Back Edge)
    !..) => State 12 (Back Edge)
    *..* => State 18 (Back Edge)
    +... => State 12 (Back Edge)
    /../ => State 20 (Back Edge)
    0..￿ => State 12 (Back Edge)

State 21 (FINAL - literal-string-exclamation):
    =..= => State 22

State 22 (FINAL - literal-string-exclamation-equals):
    (empty)

State 23:
    0x0..0x9 => State 24
    0xb..0xc => State 24
    0xe..! => State 24
    ".." => State 25
    #..[ => State 24
    \..\ => State 26
    ]..￿ => State 24

State 24:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25
    #..[ => State 24 (Back Edge)
    \..\ => State 26
    ]..￿ => State 24 (Back Edge)

State 25 (FINAL - STRING-LITERAL):
    (empty)

State 26:
    ".." => State 27
    '..' => State 27
    0..7 => State 28
    ?..? => State 27
    U..U => State 31
    \..\ => State 27
    a..a => State 27
    b..b => State 27
    f..f => State 27
    n..n => State 27
    r..r => State 27
    t..t => State 27
    u..u => State 31
    v..v => State 27
    x..x => State 36

State 27:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25 (Back Edge)
    #..[ => State 24 (Back Edge)
    \..\ => State 26 (Back Edge)
    ]..￿ => State 24 (Back Edge)

State 28:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25 (Back Edge)
    #../ => State 24 (Back Edge)
    0..7 => State 29
    8..[ => State 24 (Back Edge)
    \..\ => State 26 (Back Edge)
    ]..￿ => State 24 (Back Edge)

State 29:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25 (Back Edge)
    #../ => State 24 (Back Edge)
    0..7 => State 30
    8..[ => State 24 (Back Edge)
    \..\ => State 26 (Back Edge)
    ]..￿ => State 24 (Back Edge)

State 30:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25 (Back Edge)
    #..[ => State 24 (Back Edge)
    \..\ => State 26 (Back Edge)
    ]..￿ => State 24 (Back Edge)

State 31:
    0..9 => State 32
    A..F => State 32
    a..f => State 32

State 32:
    0..9 => State 33
    A..F => State 33
    a..f => State 33

State 33:
    0..9 => State 34
    A..F => State 34
    a..f => State 34

State 34:
    0..9 => State 35
    A..F => State 35
    a..f => State 35

State 35:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25 (Back Edge)
    #..[ => State 24 (Back Edge)
    \..\ => State 26 (Back Edge)
    ]..￿ => State 24 (Back Edge)

State 36:
    0..9 => State 37
    A..F => State 37
    a..f => State 37

State 37:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25 (Back Edge)
    #../ => State 24 (Back Edge)
    0..9 => State 38
    :..@ => State 24 (Back Edge)
    A..F => State 38
    G..[ => State 24 (Back Edge)
    \..\ => State 26 (Back Edge)
    ]..` => State 24 (Back Edge)
    a..f => State 38
    g..￿ => State 24 (Back Edge)

State 38:
    0x0..0x9 => State 24 (Back Edge)
    0xb..0xc => State 24 (Back Edge)
    0xe..! => State 24 (Back Edge)
    ".." => State 25 (Back Edge)
    #../ => State 24 (Back Edge)
    0..9 => State 38 (Back Edge)
    :..@ => State 24 (Back Edge)
    A..F => State 38 (Back Edge)
    G..[ => State 24 (Back Edge)
    \..\ => State 26 (Back Edge)
    ]..` => State 24 (Back Edge)
    a..f => State 38 (Back Edge)
    g..￿ => State 24 (Back Edge)

State 39:
    0x9..0x9 => State 40
    0x20..0x20 => State 40
    p..p => State 41

State 40:
    0x9..0x9 => State 40 (Back Edge)
    0x20..0x20 => State 40 (Back Edge)
    p..p => State 41

State 41:
    r..r => State 42

State 42:
    a..a => State 43

State 43:
    g..g => State 44

State 44:
    m..m => State 45

State 45:
    a..a => State 46

State 46:
    0x9..0x9 => State 47
    0x20..0x20 => State 47

State 47:
    0x9..0x9 => State 47 (Back Edge)
    0x20..0x20 => State 47 (Back Edge)
    a..a => State 48

State 48:
    c..c => State 49

State 49:
    c..c => State 50

State 50 (FINAL - PRAGMA_ACC):
    (empty)

State 51 (FINAL - literal-string-percent):
    (empty)

State 52 (FINAL - literal-string-ampersand):
    &..& => State 53

State 53 (FINAL - literal-string-ampersand-ampersand):
    (empty)

State 54:
    0x0..0x9 => State 55
    0xb..0xc => State 55
    0xe..& => State 55
    (..[ => State 55
    \..\ => State 57
    ]..￿ => State 55

State 55:
    '..' => State 56

State 56 (FINAL - CHARACTER-CONSTANT):
    (empty)

State 57:
    ".." => State 58
    '..' => State 58
    0..7 => State 59
    ?..? => State 58
    U..U => State 62
    \..\ => State 58
    a..a => State 58
    b..b => State 58
    f..f => State 58
    n..n => State 58
    r..r => State 58
    t..t => State 58
    u..u => State 62
    v..v => State 58
    x..x => State 67

State 58:
    '..' => State 56 (Back Edge)

State 59:
    '..' => State 56 (Back Edge)
    0..7 => State 60

State 60:
    '..' => State 56 (Back Edge)
    0..7 => State 61

State 61:
    '..' => State 56 (Back Edge)

State 62:
    0..9 => State 63
    A..F => State 63
    a..f => State 63

State 63:
    0..9 => State 64
    A..F => State 64
    a..f => State 64

State 64:
    0..9 => State 65
    A..F => State 65
    a..f => State 65

State 65:
    0..9 => State 66
    A..F => State 66
    a..f => State 66

State 66:
    '..' => State 56 (Back Edge)

State 67:
    0..9 => State 68
    A..F => State 68
    a..f => State 68

State 68:
    '..' => State 56 (Back Edge)
    0..9 => State 68 (Back Edge)
    A..F => State 68 (Back Edge)
    a..f => State 68 (Back Edge)

State 69 (FINAL - literal-string-lparen):
    (empty)

State 70 (FINAL - literal-string-rparen):
    (empty)

State 71 (FINAL - literal-string-asterisk):
    (empty)

State 72 (FINAL - literal-string-plus):
    +..+ => State 73

State 73 (FINAL - literal-string-plus-plus):
    (empty)

State 74 (FINAL - literal-string-comma):
    (empty)

State 75 (FINAL - literal-string-hyphen):
    -..- => State 76
    >..> => State 77

State 76 (FINAL - literal-string-hyphen-hyphen):
    (empty)

State 77 (FINAL - literal-string-hyphen-greaterthan):
    (empty)

State 78 (FINAL - literal-string-period):
    0..9 => State 79

State 79 (FINAL - FLOATING-CONSTANT):
    0..9 => State 79 (Back Edge)
    E..E => State 80
    F..F => State 83
    L..L => State 83
    e..e => State 80
    f..f => State 83
    l..l => State 83

State 80:
    +..+ => State 81
    -..- => State 84
    0..9 => State 82

State 81:
    0..9 => State 82

State 82 (FINAL - FLOATING-CONSTANT):
    0..9 => State 82 (Back Edge)
    F..F => State 83
    L..L => State 83
    f..f => State 83
    l..l => State 83

State 83 (FINAL - FLOATING-CONSTANT):
    (empty)

State 84:
    0..9 => State 82 (Back Edge)

State 85 (FINAL - literal-string-slash):
    *..* => State 3 (Back Edge)
    /../ => State 7 (Back Edge)

State 86 (FINAL - INTEGER-CONSTANT):
    .... => State 87
    0..7 => State 88
    8..9 => State 89
    E..E => State 90
    L..L => State 95
    U..U => State 99
    X..X => State 106
    e..e => State 90
    l..l => State 104
    u..u => State 99
    x..x => State 132

State 87 (FINAL - FLOATING-CONSTANT):
    0..9 => State 79 (Back Edge)
    E..E => State 80 (Back Edge)
    F..F => State 83 (Back Edge)
    L..L => State 83 (Back Edge)
    e..e => State 80 (Back Edge)
    f..f => State 83 (Back Edge)
    l..l => State 83 (Back Edge)

State 88 (FINAL - INTEGER-CONSTANT):
    .... => State 87 (Back Edge)
    0..7 => State 88 (Back Edge)
    8..9 => State 89
    E..E => State 90
    L..L => State 95
    U..U => State 99
    e..e => State 90
    l..l => State 104
    u..u => State 99

State 89:
    .... => State 87 (Back Edge)
    0..9 => State 89 (Back Edge)
    E..E => State 90
    e..e => State 90

State 90:
    +..+ => State 91
    -..- => State 94
    0..9 => State 92

State 91:
    0..9 => State 92

State 92 (FINAL - FLOATING-CONSTANT):
    0..9 => State 92 (Back Edge)
    F..F => State 93
    L..L => State 93
    f..f => State 93
    l..l => State 93

State 93 (FINAL - FLOATING-CONSTANT):
    (empty)

State 94:
    0..9 => State 92 (Back Edge)

State 95 (FINAL - INTEGER-CONSTANT):
    L..L => State 96
    U..U => State 98
    u..u => State 98

State 96 (FINAL - INTEGER-CONSTANT):
    U..U => State 97
    u..u => State 97

State 97 (FINAL - INTEGER-CONSTANT):
    (empty)

State 98 (FINAL - INTEGER-CONSTANT):
    (empty)

State 99 (FINAL - INTEGER-CONSTANT):
    L..L => State 100
    l..l => State 102

State 100 (FINAL - INTEGER-CONSTANT):
    L..L => State 101

State 101 (FINAL - INTEGER-CONSTANT):
    (empty)

State 102 (FINAL - INTEGER-CONSTANT):
    l..l => State 103

State 103 (FINAL - INTEGER-CONSTANT):
    (empty)

State 104 (FINAL - INTEGER-CONSTANT):
    U..U => State 98 (Back Edge)
    l..l => State 105
    u..u => State 98 (Back Edge)

State 105 (FINAL - INTEGER-CONSTANT):
    U..U => State 97 (Back Edge)
    u..u => State 97 (Back Edge)

State 106:
    .... => State 107
    0..9 => State 114
    A..F => State 114
    a..f => State 114

State 107:
    0..9 => State 108
    A..F => State 108
    a..f => State 108

State 108:
    0..9 => State 108 (Back Edge)
    A..F => State 108 (Back Edge)
    P..P => State 109
    a..f => State 108 (Back Edge)
    p..p => State 109

State 109:
    +..+ => State 110
    -..- => State 113
    0..9 => State 111

State 110:
    0..9 => State 111

State 111 (FINAL - FLOATING-CONSTANT):
    0..9 => State 111 (Back Edge)
    F..F => State 112
    L..L => State 112
    f..f => State 112
    l..l => State 112

State 112 (FINAL - FLOATING-CONSTANT):
    (empty)

State 113:
    0..9 => State 111 (Back Edge)

State 114 (FINAL - INTEGER-CONSTANT):
    .... => State 115
    0..9 => State 114 (Back Edge)
    A..F => State 114 (Back Edge)
    L..L => State 116
    P..P => State 120
    U..U => State 125
    a..f => State 114 (Back Edge)
    l..l => State 130
    p..p => State 120
    u..u => State 125

State 115:
    0..9 => State 108 (Back Edge)
    A..F => State 108 (Back Edge)
    P..P => State 109 (Back Edge)
    a..f => State 108 (Back Edge)
    p..p => State 109 (Back Edge)

State 116 (FINAL - INTEGER-CONSTANT):
    L..L => State 117
    U..U => State 119
    u..u => State 119

State 117 (FINAL - INTEGER-CONSTANT):
    U..U => State 118
    u..u => State 118

State 118 (FINAL - INTEGER-CONSTANT):
    (empty)

State 119 (FINAL - INTEGER-CONSTANT):
    (empty)

State 120:
    +..+ => State 121
    -..- => State 124
    0..9 => State 122

State 121:
    0..9 => State 122

State 122 (FINAL - FLOATING-CONSTANT):
    0..9 => State 122 (Back Edge)
    F..F => State 123
    L..L => State 123
    f..f => State 123
    l..l => State 123

State 123 (FINAL - FLOATING-CONSTANT):
    (empty)

State 124:
    0..9 => State 122 (Back Edge)

State 125 (FINAL - INTEGER-CONSTANT):
    L..L => State 126
    l..l => State 128

State 126 (FINAL - INTEGER-CONSTANT):
    L..L => State 127

State 127 (FINAL - INTEGER-CONSTANT):
    (empty)

State 128 (FINAL - INTEGER-CONSTANT):
    l..l => State 129

State 129 (FINAL - INTEGER-CONSTANT):
    (empty)

State 130 (FINAL - INTEGER-CONSTANT):
    U..U => State 119 (Back Edge)
    l..l => State 131
    u..u => State 119 (Back Edge)

State 131 (FINAL - INTEGER-CONSTANT):
    U..U => State 118 (Back Edge)
    u..u => State 118 (Back Edge)

State 132:
    .... => State 107 (Back Edge)
    0..9 => State 114 (Back Edge)
    A..F => State 114 (Back Edge)
    a..f => State 114 (Back Edge)

State 133 (FINAL - INTEGER-CONSTANT):
    .... => State 87 (Back Edge)
    0..9 => State 134
    E..E => State 90 (Back Edge)
    L..L => State 135
    U..U => State 139
    e..e => State 90 (Back Edge)
    l..l => State 144
    u..u => State 139

State 134 (FINAL - INTEGER-CONSTANT):
    .... => State 87 (Back Edge)
    0..9 => State 134 (Back Edge)
    E..E => State 90 (Back Edge)
    L..L => State 135
    U..U => State 139
    e..e => State 90 (Back Edge)
    l..l => State 144
    u..u => State 139

State 135 (FINAL - INTEGER-CONSTANT):
    L..L => State 136
    U..U => State 138
    u..u => State 138

State 136 (FINAL - INTEGER-CONSTANT):
    U..U => State 137
    u..u => State 137

State 137 (FINAL - INTEGER-CONSTANT):
    (empty)

State 138 (FINAL - INTEGER-CONSTANT):
    (empty)

State 139 (FINAL - INTEGER-CONSTANT):
    L..L => State 140
    l..l => State 142

State 140 (FINAL - INTEGER-CONSTANT):
    L..L => State 141

State 141 (FINAL - INTEGER-CONSTANT):
    (empty)

State 142 (FINAL - INTEGER-CONSTANT):
    l..l => State 143

State 143 (FINAL - INTEGER-CONSTANT):
    (empty)

State 144 (FINAL - INTEGER-CONSTANT):
    U..U => State 138 (Back Edge)
    l..l => State 145
    u..u => State 138 (Back Edge)

State 145 (FINAL - INTEGER-CONSTANT):
    U..U => State 137 (Back Edge)
    u..u => State 137 (Back Edge)

State 146 (FINAL - literal-string-colon):
    (empty)

State 147 (FINAL - literal-string-lessthan):
    <..< => State 148
    =..= => State 149

State 148 (FINAL - literal-string-lessthan-lessthan):
    (empty)

State 149 (FINAL - literal-string-lessthan-equals):
    (empty)

State 150:
    =..= => State 151

State 151 (FINAL - literal-string-equals-equals):
    (empty)

State 152 (FINAL - literal-string-greaterthan):
    =..= => State 153
    >..> => State 154

State 153 (FINAL - literal-string-greaterthan-equals):
    (empty)

State 154 (FINAL - literal-string-greaterthan-greaterthan):
    (empty)

State 155 (FINAL - literal-string-question):
    (empty)

State 156 (FINAL - IDENTIFIER):
    0..9 => State 157
    A..Z => State 157
    \..\ => State 158
    _.._ => State 157
    a..z => State 157

State 157 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 158:
    U..U => State 159
    u..u => State 159

State 159:
    0..9 => State 160
    A..F => State 160
    a..f => State 160

State 160:
    0..9 => State 161
    A..F => State 161
    a..f => State 161

State 161:
    0..9 => State 162
    A..F => State 162
    a..f => State 162

State 162:
    0..9 => State 163
    A..F => State 163
    a..f => State 163

State 163 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 164 (FINAL - IDENTIFIER):
    ".." => State 23 (Back Edge)
    '..' => State 54 (Back Edge)
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 165 (FINAL - literal-string-lbracket):
    (empty)

State 166:
    U..U => State 167
    u..u => State 167

State 167:
    0..9 => State 168
    A..F => State 168
    a..f => State 168

State 168:
    0..9 => State 169
    A..F => State 169
    a..f => State 169

State 169:
    0..9 => State 170
    A..F => State 170
    a..f => State 170

State 170:
    0..9 => State 171
    A..F => State 171
    a..f => State 171

State 171 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 172 (FINAL - literal-string-rbracket):
    (empty)

State 173 (FINAL - literal-string-caret):
    (empty)

State 174 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 175
    t..z => State 157 (Back Edge)

State 175 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..x => State 157 (Back Edge)
    y..y => State 176
    z..z => State 157 (Back Edge)

State 176 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 177
    o..z => State 157 (Back Edge)

State 177 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 178
    d..z => State 157 (Back Edge)

State 178 (FINAL - literal-string-async):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 179 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 180
    b..n => State 157 (Back Edge)
    o..o => State 184
    p..q => State 157 (Back Edge)
    r..r => State 198
    s..z => State 157 (Back Edge)

State 180 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 181
    d..z => State 157 (Back Edge)

State 181 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..g => State 157 (Back Edge)
    h..h => State 182
    i..z => State 157 (Back Edge)

State 182 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 183
    f..z => State 157 (Back Edge)

State 183 (FINAL - literal-string-cache):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 184 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 185
    m..o => State 157 (Back Edge)
    p..p => State 191
    q..z => State 157 (Back Edge)

State 185 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 186
    m..z => State 157 (Back Edge)

State 186 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 187
    b..z => State 157 (Back Edge)

State 187 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..o => State 157 (Back Edge)
    p..p => State 188
    q..z => State 157 (Back Edge)

State 188 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 189
    t..z => State 157 (Back Edge)

State 189 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 190
    f..z => State 157 (Back Edge)

State 190 (FINAL - literal-string-collapse):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 191 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..x => State 157 (Back Edge)
    y..y => State 192
    z..z => State 157 (Back Edge)

State 192 (FINAL - literal-string-copy):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 193
    j..n => State 157 (Back Edge)
    o..o => State 195
    p..z => State 157 (Back Edge)

State 193 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 194
    o..z => State 157 (Back Edge)

State 194 (FINAL - literal-string-copyin):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 195 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..t => State 157 (Back Edge)
    u..u => State 196
    v..z => State 157 (Back Edge)

State 196 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 197
    u..z => State 157 (Back Edge)

State 197 (FINAL - literal-string-copyout):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 198 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 199
    f..z => State 157 (Back Edge)

State 199 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 200
    b..z => State 157 (Back Edge)

State 200 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 201
    u..z => State 157 (Back Edge)

State 201 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 202
    f..z => State 157 (Back Edge)

State 202 (FINAL - literal-string-create):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 203 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 204
    b..d => State 157 (Back Edge)
    e..e => State 207
    f..z => State 157 (Back Edge)

State 204 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 205
    u..z => State 157 (Back Edge)

State 205 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 206
    b..z => State 157 (Back Edge)

State 206 (FINAL - literal-string-data):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 207 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 208
    d..u => State 157 (Back Edge)
    v..v => State 213
    w..z => State 157 (Back Edge)

State 208 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 209
    m..z => State 157 (Back Edge)

State 209 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 210
    b..z => State 157 (Back Edge)

State 210 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 211
    s..z => State 157 (Back Edge)

State 211 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 212
    f..z => State 157 (Back Edge)

State 212 (FINAL - literal-string-declare):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 213 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 214
    j..z => State 157 (Back Edge)

State 214 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 215
    d..z => State 157 (Back Edge)

State 215 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 216
    f..z => State 157 (Back Edge)

State 216 (FINAL - literal-string-device):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 217
    a..o => State 157 (Back Edge)
    p..p => State 226
    q..z => State 157 (Back Edge)

State 217 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 218
    s..z => State 157 (Back Edge)

State 218 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 219
    f..z => State 157 (Back Edge)

State 219 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 220
    t..z => State 157 (Back Edge)

State 220 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 221
    j..z => State 157 (Back Edge)

State 221 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..c => State 157 (Back Edge)
    d..d => State 222
    e..z => State 157 (Back Edge)

State 222 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 223
    f..z => State 157 (Back Edge)

State 223 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 224
    o..z => State 157 (Back Edge)

State 224 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 225
    u..z => State 157 (Back Edge)

State 225 (FINAL - literal-string-device-underscoreresident):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 226 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 227
    u..z => State 157 (Back Edge)

State 227 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 228
    s..z => State 157 (Back Edge)

State 228 (FINAL - literal-string-deviceptr):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 229 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 230
    j..z => State 157 (Back Edge)

State 230 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 231
    s..z => State 157 (Back Edge)

State 231 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 232
    t..z => State 157 (Back Edge)

State 232 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 233
    u..z => State 157 (Back Edge)

State 233 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..o => State 157 (Back Edge)
    p..p => State 234
    q..z => State 157 (Back Edge)

State 234 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 235
    s..z => State 157 (Back Edge)

State 235 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 236
    j..z => State 157 (Back Edge)

State 236 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..u => State 157 (Back Edge)
    v..v => State 237
    w..z => State 157 (Back Edge)

State 237 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 238
    b..z => State 157 (Back Edge)

State 238 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 239
    u..z => State 157 (Back Edge)

State 239 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 240
    f..z => State 157 (Back Edge)

State 240 (FINAL - literal-string-firstprivate):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 241 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 242
    b..z => State 157 (Back Edge)

State 242 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 243
    o..z => State 157 (Back Edge)

State 243 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..f => State 157 (Back Edge)
    g..g => State 244
    h..z => State 157 (Back Edge)

State 244 (FINAL - literal-string-gang):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 245 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 246
    p..z => State 157 (Back Edge)

State 246 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 247
    t..z => State 157 (Back Edge)

State 247 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 248
    u..z => State 157 (Back Edge)

State 248 (FINAL - literal-string-host):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 249
    a..z => State 157 (Back Edge)

State 249 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..c => State 157 (Back Edge)
    d..d => State 250
    e..z => State 157 (Back Edge)

State 250 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 251
    b..z => State 157 (Back Edge)

State 251 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 252
    u..z => State 157 (Back Edge)

State 252 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 253
    b..z => State 157 (Back Edge)

State 253 (FINAL - literal-string-host-underscoredata):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 254 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..e => State 157 (Back Edge)
    f..f => State 255
    g..m => State 157 (Back Edge)
    n..n => State 256
    o..z => State 157 (Back Edge)

State 255 (FINAL - literal-string-if):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 256 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..c => State 157 (Back Edge)
    d..d => State 257
    e..z => State 157 (Back Edge)

State 257 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 258
    f..z => State 157 (Back Edge)

State 258 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..o => State 157 (Back Edge)
    p..p => State 259
    q..z => State 157 (Back Edge)

State 259 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 260
    f..z => State 157 (Back Edge)

State 260 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 261
    o..z => State 157 (Back Edge)

State 261 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..c => State 157 (Back Edge)
    d..d => State 262
    e..z => State 157 (Back Edge)

State 262 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 263
    f..z => State 157 (Back Edge)

State 263 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 264
    o..z => State 157 (Back Edge)

State 264 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 265
    u..z => State 157 (Back Edge)

State 265 (FINAL - literal-string-independent):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 266 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 267
    f..z => State 157 (Back Edge)

State 267 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 268
    s..z => State 157 (Back Edge)

State 268 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 269
    o..z => State 157 (Back Edge)

State 269 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 270
    f..z => State 157 (Back Edge)

State 270 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 271
    m..z => State 157 (Back Edge)

State 271 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 272
    t..z => State 157 (Back Edge)

State 272 (FINAL - literal-string-kernels):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 273 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 274
    p..z => State 157 (Back Edge)

State 274 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 275
    p..z => State 157 (Back Edge)

State 275 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..o => State 157 (Back Edge)
    p..p => State 276
    q..z => State 157 (Back Edge)

State 276 (FINAL - literal-string-loop):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 277 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 278
    b..h => State 157 (Back Edge)
    i..i => State 280
    j..z => State 157 (Back Edge)

State 278 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..w => State 157 (Back Edge)
    x..x => State 279
    y..z => State 157 (Back Edge)

State 279 (FINAL - literal-string-max):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 280 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 281
    o..z => State 157 (Back Edge)

State 281 (FINAL - literal-string-min):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 282 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..t => State 157 (Back Edge)
    u..u => State 283
    v..z => State 157 (Back Edge)

State 283 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..l => State 157 (Back Edge)
    m..m => State 284
    n..z => State 157 (Back Edge)

State 284 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 285
    a..z => State 157 (Back Edge)

State 285 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..f => State 157 (Back Edge)
    g..g => State 286
    h..v => State 157 (Back Edge)
    w..w => State 291
    x..z => State 157 (Back Edge)

State 286 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 287
    b..z => State 157 (Back Edge)

State 287 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 288
    o..z => State 157 (Back Edge)

State 288 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..f => State 157 (Back Edge)
    g..g => State 289
    h..z => State 157 (Back Edge)

State 289 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 290
    t..z => State 157 (Back Edge)

State 290 (FINAL - literal-string-num-underscoregangs):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 291 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 292
    p..z => State 157 (Back Edge)

State 292 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 293
    s..z => State 157 (Back Edge)

State 293 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..j => State 157 (Back Edge)
    k..k => State 294
    l..z => State 157 (Back Edge)

State 294 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 295
    f..z => State 157 (Back Edge)

State 295 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 296
    s..z => State 157 (Back Edge)

State 296 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 297
    t..z => State 157 (Back Edge)

State 297 (FINAL - literal-string-num-underscoreworkers):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 298 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 299
    b..b => State 157 (Back Edge)
    c..c => State 306
    d..q => State 157 (Back Edge)
    r..r => State 320
    s..z => State 157 (Back Edge)

State 299 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 300
    s..z => State 157 (Back Edge)

State 300 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 301
    b..z => State 157 (Back Edge)

State 301 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 302
    m..z => State 157 (Back Edge)

State 302 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 303
    m..z => State 157 (Back Edge)

State 303 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 304
    f..z => State 157 (Back Edge)

State 304 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 305
    m..z => State 157 (Back Edge)

State 305 (FINAL - literal-string-parallel):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 306 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 307
    p..q => State 157 (Back Edge)
    r..r => State 315
    s..z => State 157 (Back Edge)

State 307 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..o => State 157 (Back Edge)
    p..p => State 308
    q..z => State 157 (Back Edge)

State 308 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..x => State 157 (Back Edge)
    y..y => State 309
    z..z => State 157 (Back Edge)

State 309 (FINAL - literal-string-pcopy):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 310
    j..n => State 157 (Back Edge)
    o..o => State 312
    p..z => State 157 (Back Edge)

State 310 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 311
    o..z => State 157 (Back Edge)

State 311 (FINAL - literal-string-pcopyin):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 312 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..t => State 157 (Back Edge)
    u..u => State 313
    v..z => State 157 (Back Edge)

State 313 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 314
    u..z => State 157 (Back Edge)

State 314 (FINAL - literal-string-pcopyout):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 315 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 316
    f..z => State 157 (Back Edge)

State 316 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 317
    b..z => State 157 (Back Edge)

State 317 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 318
    u..z => State 157 (Back Edge)

State 318 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 319
    f..z => State 157 (Back Edge)

State 319 (FINAL - literal-string-pcreate):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 320 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 321
    f..h => State 157 (Back Edge)
    i..i => State 344
    j..z => State 157 (Back Edge)

State 321 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..r => State 157 (Back Edge)
    s..s => State 322
    t..z => State 157 (Back Edge)

State 322 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 323
    f..z => State 157 (Back Edge)

State 323 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 324
    o..z => State 157 (Back Edge)

State 324 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 325
    u..z => State 157 (Back Edge)

State 325 (FINAL - literal-string-present):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 326
    a..z => State 157 (Back Edge)

State 326 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 327
    p..z => State 157 (Back Edge)

State 327 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 328
    s..z => State 157 (Back Edge)

State 328 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 329
    a..z => State 157 (Back Edge)

State 329 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 330
    d..z => State 157 (Back Edge)

State 330 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 331
    p..q => State 157 (Back Edge)
    r..r => State 339
    s..z => State 157 (Back Edge)

State 331 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..o => State 157 (Back Edge)
    p..p => State 332
    q..z => State 157 (Back Edge)

State 332 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..x => State 157 (Back Edge)
    y..y => State 333
    z..z => State 157 (Back Edge)

State 333 (FINAL - literal-string-present-underscoreor-underscorecopy):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 334
    j..n => State 157 (Back Edge)
    o..o => State 336
    p..z => State 157 (Back Edge)

State 334 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 335
    o..z => State 157 (Back Edge)

State 335 (FINAL - literal-string-present-underscoreor-underscorecopyin):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 336 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..t => State 157 (Back Edge)
    u..u => State 337
    v..z => State 157 (Back Edge)

State 337 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 338
    u..z => State 157 (Back Edge)

State 338 (FINAL - literal-string-present-underscoreor-underscorecopyout):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 339 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 340
    f..z => State 157 (Back Edge)

State 340 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 341
    b..z => State 157 (Back Edge)

State 341 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 342
    u..z => State 157 (Back Edge)

State 342 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 343
    f..z => State 157 (Back Edge)

State 343 (FINAL - literal-string-present-underscoreor-underscorecreate):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 344 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..u => State 157 (Back Edge)
    v..v => State 345
    w..z => State 157 (Back Edge)

State 345 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 346
    b..z => State 157 (Back Edge)

State 346 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 347
    u..z => State 157 (Back Edge)

State 347 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 348
    f..z => State 157 (Back Edge)

State 348 (FINAL - literal-string-private):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 349 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 350
    f..z => State 157 (Back Edge)

State 350 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..c => State 157 (Back Edge)
    d..d => State 351
    e..z => State 157 (Back Edge)

State 351 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..t => State 157 (Back Edge)
    u..u => State 352
    v..z => State 157 (Back Edge)

State 352 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 353
    d..z => State 157 (Back Edge)

State 353 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 354
    u..z => State 157 (Back Edge)

State 354 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 355
    j..z => State 157 (Back Edge)

State 355 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 356
    p..z => State 157 (Back Edge)

State 356 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 357
    o..z => State 157 (Back Edge)

State 357 (FINAL - literal-string-reduction):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 358 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 359
    f..h => State 157 (Back Edge)
    i..i => State 361
    j..z => State 157 (Back Edge)

State 359 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..p => State 157 (Back Edge)
    q..q => State 360
    r..z => State 157 (Back Edge)

State 360 (FINAL - literal-string-seq):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 361 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..y => State 157 (Back Edge)
    z..z => State 362

State 362 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 363
    f..z => State 157 (Back Edge)

State 363 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 364
    p..z => State 157 (Back Edge)

State 364 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..e => State 157 (Back Edge)
    f..f => State 365
    g..z => State 157 (Back Edge)

State 365 (FINAL - literal-string-sizeof):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 366 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..o => State 157 (Back Edge)
    p..p => State 367
    q..r => State 157 (Back Edge)
    s..s => State 372
    t..z => State 157 (Back Edge)

State 367 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..c => State 157 (Back Edge)
    d..d => State 368
    e..z => State 157 (Back Edge)

State 368 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 369
    b..z => State 157 (Back Edge)

State 369 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 370
    u..z => State 157 (Back Edge)

State 370 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 371
    f..z => State 157 (Back Edge)

State 371 (FINAL - literal-string-update):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 372 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 373
    f..z => State 157 (Back Edge)

State 373 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 374
    a..z => State 157 (Back Edge)

State 374 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..c => State 157 (Back Edge)
    d..d => State 375
    e..z => State 157 (Back Edge)

State 375 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 376
    f..z => State 157 (Back Edge)

State 376 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..u => State 157 (Back Edge)
    v..v => State 377
    w..z => State 157 (Back Edge)

State 377 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 378
    j..z => State 157 (Back Edge)

State 378 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 379
    d..z => State 157 (Back Edge)

State 379 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 380
    f..z => State 157 (Back Edge)

State 380 (FINAL - literal-string-use-underscoredevice):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 381 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 382
    f..z => State 157 (Back Edge)

State 382 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..b => State 157 (Back Edge)
    c..c => State 383
    d..z => State 157 (Back Edge)

State 383 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 384
    u..z => State 157 (Back Edge)

State 384 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..n => State 157 (Back Edge)
    o..o => State 385
    p..z => State 157 (Back Edge)

State 385 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 386
    s..z => State 157 (Back Edge)

State 386 (FINAL - literal-string-vector):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 387
    a..z => State 157 (Back Edge)

State 387 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..k => State 157 (Back Edge)
    l..l => State 388
    m..z => State 157 (Back Edge)

State 388 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 389
    f..z => State 157 (Back Edge)

State 389 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..m => State 157 (Back Edge)
    n..n => State 390
    o..z => State 157 (Back Edge)

State 390 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..f => State 157 (Back Edge)
    g..g => State 391
    h..z => State 157 (Back Edge)

State 391 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 392
    u..z => State 157 (Back Edge)

State 392 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..g => State 157 (Back Edge)
    h..h => State 393
    i..z => State 157 (Back Edge)

State 393 (FINAL - literal-string-vector-underscorelength):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 394 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..a => State 395
    b..n => State 157 (Back Edge)
    o..o => State 398
    p..z => State 157 (Back Edge)

State 395 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..h => State 157 (Back Edge)
    i..i => State 396
    j..z => State 157 (Back Edge)

State 396 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..s => State 157 (Back Edge)
    t..t => State 397
    u..z => State 157 (Back Edge)

State 397 (FINAL - literal-string-wait):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 398 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 399
    s..z => State 157 (Back Edge)

State 399 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..j => State 157 (Back Edge)
    k..k => State 400
    l..z => State 157 (Back Edge)

State 400 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..d => State 157 (Back Edge)
    e..e => State 401
    f..z => State 157 (Back Edge)

State 401 (FINAL - IDENTIFIER):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..q => State 157 (Back Edge)
    r..r => State 402
    s..z => State 157 (Back Edge)

State 402 (FINAL - literal-string-worker):
    0..9 => State 157 (Back Edge)
    A..Z => State 157 (Back Edge)
    \..\ => State 158 (Back Edge)
    _.._ => State 157 (Back Edge)
    a..z => State 157 (Back Edge)

State 403 (FINAL - literal-string-vbar):
    |..| => State 404

State 404 (FINAL - literal-string-vbar-vbar):
    (empty)

State 405 (FINAL - literal-string-tilde):
    (empty)


*/

package edu.auburn.oaccrefac.core.parser;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.zip.Inflater;

import edu.auburn.oaccrefac.core.parser.OpenACCParser.Terminal;
import edu.auburn.oaccrefac.core.parser.OpenACCParser.ILexer;
import edu.auburn.oaccrefac.core.parser.Token;

@SuppressWarnings("all")
public final class Lexer implements ILexer
{
    public static void main(String[] args) throws Exception
    {
        Lexer lexer = new Lexer(System.in);
        for (Token t = lexer.getNextToken(); t != null && t.getTerminal() != Terminal.END_OF_INPUT; t = lexer.getNextToken())
            System.out.println(t);
    }

    protected static final int NUM_TERMINALS = 82;

    protected final Reader input;
    protected StringBuilder currentTokenText;
    protected StringBuilder leftoverInput;
    protected int leftoverInputPos;
    protected int nextChar;
    protected int nextCharOffset;
    protected int nextCharLine, nextCharCol;
    protected int lastTokenLine, lastTokenCol;

    protected Token previousToken = null;

    public Lexer(String input)
    {
        this(new StringReader(input));
    }

    public Lexer(char[] input)
    {
        this(new CharArrayReader(input));
    }

    public Lexer(File input) throws FileNotFoundException
    {
        this(new BufferedReader(new FileReader(input)));
    }

    public Lexer(InputStream input)
    {
        this(new BufferedReader(new InputStreamReader(input)));
    }

    public Lexer(Reader input)
    {
        if (input == null) throw new IllegalArgumentException("input cannot be null");

        this.input = input;
        this.leftoverInput = new StringBuilder();
        this.leftoverInputPos = 0;
        this.currentTokenText = new StringBuilder(256);
        this.nextChar = 0;
        this.nextCharOffset = 0;
        this.nextCharLine = 1;
        this.nextCharCol = 1;
        this.lastTokenLine = 1;
        this.lastTokenCol = 1;
    }

    public Token yylex() throws Exception
    {
        return getNextToken();
    }

    public Token getNextToken() throws Exception
    {
        StringBuilder whiteText = new StringBuilder();
        Token tok = internalGetNextToken();
        while (tok != null && tok.getTerminal() == Terminal.SKIP)
        {
            if (tok != null) whiteText.append(tok.getText());
            tok = internalGetNextToken();
        }
        if (tok != null)
        {
            if (tok.getTerminal() == Terminal.END_OF_INPUT && previousToken != null)
                previousToken.setWhiteAfter(whiteText.toString());
            else
                tok.setWhiteBefore(whiteText.toString());
        }

        previousToken = tok;
        return tok;
    }

    public String describeLastTokenPos()
    {
        return " (line " + lastTokenLine + ", column " + lastTokenCol + ")";
    }

    public Token internalGetNextToken() throws Exception
    {
        lastTokenLine = nextCharLine;
        lastTokenCol = nextCharCol;

        int startCharOffset = nextCharOffset;

        int currentState = 0;
        int posFollowingAcceptedSubstring = this.nextCharOffset;
        int lineFollowingAcceptedSubstring = this.nextCharLine;
        int colFollowingAcceptedSubstring = this.nextCharCol;

        int curChar;
        int curLine;
        int curCol;
        Terminal accept = null;

        if (nextCharOffset == 0 && nextChar == 0) // First call to this method
            nextChar = this.input.read();

        if (nextChar < 0)
            return new Token(Terminal.END_OF_INPUT, "(end of input)", this.nextCharOffset);

        while (true)
        {
            assert this.nextChar >= 0;

            curChar = this.nextChar;
            curLine = this.nextCharLine;
            curCol = this.nextCharCol;

            currentTokenText.append((char)curChar);

            currentState = DFATransitionTable.get(currentState, dfaTableColumn[curChar]);
            if (currentState < 0) break;

            this.nextCharOffset++;
            if (this.leftoverInputPos < this.leftoverInput.length())
                this.nextChar = this.leftoverInput.charAt(this.leftoverInputPos++);
            else
                this.nextChar = this.input.read();

            if (this.nextChar >= 0)
            {
                if (nextChar == '\n')
                {
                    this.nextCharLine++;
                    this.nextCharCol = 1;
                }
                else if (nextChar == '\r')
                {
                    this.nextCharCol = 1;
                }
                else
                {
                    this.nextCharCol++;
                }
            }

            if (acceptTerminal[currentState] != null)
            {
                posFollowingAcceptedSubstring = this.nextCharOffset;
                lineFollowingAcceptedSubstring = this.nextCharLine;
                colFollowingAcceptedSubstring = this.nextCharCol;
                accept = acceptTerminal[currentState];
            }

            if (this.nextChar < 0) break;
        }

        if (accept != null)
        {
            int length = posFollowingAcceptedSubstring-startCharOffset;

            Token result = new Token(
                accept,
                currentTokenText.substring(0, length),
                startCharOffset);

            if (this.nextCharOffset > posFollowingAcceptedSubstring)
            {
                String suffix = this.leftoverInput.substring(this.leftoverInputPos);
                this.leftoverInput = new StringBuilder();
                this.leftoverInput.append(currentTokenText.substring(length+1)); // nextChar set below, so start at second unread character
                this.leftoverInput.append(suffix);
                this.leftoverInputPos = 0;

                this.nextChar = currentTokenText.charAt(length);

                this.currentTokenText = new StringBuilder(256);
            }
            else
            {
                currentTokenText.delete(0, currentTokenText.length());
            }

            this.nextCharOffset = posFollowingAcceptedSubstring;
            this.nextCharLine = lineFollowingAcceptedSubstring;
            this.nextCharCol = colFollowingAcceptedSubstring;
            return result;
        }
        else
        {
            syntaxError(curChar, curLine, curCol);
            return null;
        }
    }

    protected void syntaxError(int curChar, int curLine, int curCol) throws Exception
    {
        throw new Exception("Unexpected " + describeChar(curChar) + " at line " + curLine + ", column " + curCol);
    }

    protected String describeChar(int ch)
    {
        if (ch < 0)
            return "end of input";
        else if (ch == 10 || ch == 13)
            return "end of line";
        else if (ch < 32)
            return "character 0x" + Integer.toHexString(ch).toUpperCase();
        else
            return "character '" + (char)ch + "'";
    }


    protected static final int[] dfaTableColumn;

    static
    {
        final int[] partitionPoints =
        {
                0,     9,    10,    11,    13,    14,    32,    33,    34,    35,    36,    37,    38,    39,    40,    41,    42,    43,    44,    45,    46,    47,    48,    49,    56,
               58,    59,    60,    61,    62,    63,    64,    65,    69,    70,    71,    76,    77,    80,    81,    85,    86,    88,    89,    91,    92,    93,    94,    95,    96,
               97,    98,    99,   100,   101,   102,   103,   104,   105,   106,   107,   108,   109,   110,   111,   112,   113,   114,   115,   116,   117,   118,   119,   120,   121,
              122,   123,   124,   125,   126,   127,  65536,
        };

        dfaTableColumn = new int[65536];

        for (int j = 0; j < partitionPoints[0]; j++)
            dfaTableColumn[j] = 0;

        for (int i = 0; i < partitionPoints.length-1; i++)
            for (int j = partitionPoints[i]; j < partitionPoints[i+1]; j++)
                dfaTableColumn[j] = i+1;

        int i = partitionPoints.length-1;
        for (int j = partitionPoints[i]; j < 65536; j++)
            dfaTableColumn[j] = i+1;
    }

 /*
    protected static final int[][] dfaTransitionTable =
    {
    };
 */
    protected static final class DFATransitionTable
    {
        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 1, 0, 21, 22, 0, 1, 23, 24, 25, 26, 2, 6, 27, 28, 29, 30, 31, 32, 2, 27, 2, 33, 1, 1, 34, 28, 35, 1, 33, 0, 0, 1, 0, 36, 2, 0, 37, 6, 33, 34, 27, 38, 39, 40, 41, 28, 42, 43, 0, 0, 0, 1, 0, 0, 1, 0, 0, 35, 44, 45, 46, 47, 0, 48, 27, 49, 50, 51, 52, 53, 54, 55, 0, 56, 2, 6, 0, 0, 1, 6, 0, 2, 0, 27, 28, 57, 58, 59, 60, 61, 62, 0, 63, 64, 65, 30, 33, 0, 0, 66, 67, 68, 0, 69, 28, 27, 0, 6, 0, 34, 35, 70, 71, 72, 38, 39, 0, 0, 33, 34, 0, 30, 0, 40, 41, 0, 2, 0, 0, 6, 0, 27, 0, 0, 0, 73, 74, 42, 75, 76, 77, 78, 79, 80, 0, 43, 81, 82, 83, 84, 85, 0, 0, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 1, 0, 0 };
        protected static final int[] columnmap = { 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 0 };

        public static int get(int row, int col)
        {
            if (isErrorEntry(row, col))
                return -1;
            else if (columnmap[col] % 2 == 0)
                return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
            else
                return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
        }

        protected static boolean isErrorEntry(int row, int col)
        {
            final int INT_BITS = 32;
            int sigmapRow = row;

            int sigmapCol = col / INT_BITS;
            int bitNumberFromLeft = col % INT_BITS;
            int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

            return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
        }

        protected static int[][] sigmap = null;

        protected static void sigmapInit()
        {
            try
            {
                final int rows = 406;
                final int cols = 3;
                final int compressedBytes = 320;
                final int uncompressedBytes = 4873;
                
                byte[] decoded = new byte[compressedBytes];
                base64Decode(decoded,
                    "eNrtl7FqwzAQhk8iLhkydOiQLRJ06d4HEHqCvEEeIEszdLbrtS" +
                    "/RR9Gb9BGylkBQBU6ck+xLJNxChvvB8CHf/fKdzwLrvd/X/tv7" +
                    "QwMaJFxke6r9WY5knLtF6zlcF/Jf+QTNYESUfyQrPsAum8+v8n" +
                    "oBhHuDVdMZ3WYql/LXp0dU4cJ81rynR1yRQSwgidHoph7kzgge" +
                    "EXF7e0Q1SiIX9TyKkaIZZ5xc1nMyV+L4HKmMmLm8HSMc5lcQbc" +
                    "cvjop5Amiv+1i0XrnntZBCpf5h3YT1E6t+NKbsG8bAoCEz0cBR" +
                    "YyMzJmy8jwbkcK+KnIFdYDW5xsq9m1WrB56ltU/s87/2LZ6NC9" +
                    "/D+420yPgIqW0foEjC1YeN9z8p42crPIuwj008kxN8sn9xXczM" +
                    "zMzMzMzMzMz3xlf/Hjr9Aho/g94=");
                
                byte[] buffer = new byte[uncompressedBytes];
                Inflater inflater = new Inflater();
                inflater.setInput(decoded, 0, compressedBytes);
                inflater.inflate(buffer);
                inflater.end();
                
                sigmap = new int[rows][cols];
                for (int index = 0; index < uncompressedBytes-1; index += 4)
                {
                    int byte1 = 0x000000FF & (int)buffer[index + 0];
                    int byte2 = 0x000000FF & (int)buffer[index + 1];
                    int byte3 = 0x000000FF & (int)buffer[index + 2];
                    int byte4 = 0x000000FF & (int)buffer[index + 3];
                    
                    int element = index / 4;
                    int row = element / cols;
                    int col = element % cols;
                    sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
                }
            }
            catch (Exception e)
            {
                throw new Error(e);
            }
        }

        protected static int lookupSigmap(int row, int col)
        {
            return sigmap[row][col];
        }

        protected static int[][] value = null;

        protected static void valueInit()
        {
            try
            {
                final int rows = 120;
                final int cols = 41;
                final int compressedBytes = 1209;
                final int uncompressedBytes = 19681;
                
                byte[] decoded = new byte[compressedBytes];
                base64Decode(decoded,
                    "eNrtWeef1UQUPcGOoKIiyKIIKCI2bKBSFBQbFgQVWVwVUOyIqI" +
                    "Diil1ZxAYq4Cp2VFDAjoK99y/+Cf4BfOCr8c5kNgnJnWSeSfZN" +
                    "3ibnd2funLl39uydTPblrevCcdUFB73RB4eTdwJOxAhMwFk4G+" +
                    "fgPEzEhbgUl+FhwjM0vxzPYSVeIK9d4XXf8/AG3sR6vIt2inmP" +
                    "xh/gV2r/wTZsx79od7o7vZzeTj/yhpG1OLOpneO0Om1evus6y8" +
                    "me7VAWaCRrUv5Jvncu2flk3dAf/f3YA3BBR4QcX6P6AT7TFHiE" +
                    "45R3lM/OxtH+z2iScwNkf6jHOStCqw+Ne3J0smx38se74RAB6a" +
                    "9Ax2+oOFytxjPd2OXFBNmKvU71R5AN0+VKfucU7JIaUTAaTuOu" +
                    "9dIYPTPR+zE07oaBAmr0vOoVh2vVeAZzPw0MEGLvj8XN0NyPu2" +
                    "MP0XoemR7dE2cLQ6Wx0zTu6Xv71E1jD/Sk09NDWk+yKPbyvV7M" +
                    "bCeg0lgHjXvXTWMZzkwjnOt9fW//EtyP+1mrsQx1tEFjX/QVre" +
                    "cRDlR9dvTLZ51KY100HkSwXePBhGqvWY01fQcwSECNVmG17BWH" +
                    "xQnfAQwKEGJvCL4D0OeqmWPjHvdeSMxgAZ7DvQnvhYMDhNj7jN" +
                    "8LS7HX5hcOE+A53KnGC3R50Ww8GYtbkEMdh0ikc9E6Dgmh6HNd" +
                    "J40JOxs/M6MxWhv9uOrnM3NHkh0fY5+IMfPdGi9G4xiM0UYvS7" +
                    "gfj2EzHjS9H/XPHibyIoGUGK6Oww2roqkjRmKkaD0vjDhTI07J" +
                    "mO/riGk+Ne75zCiM4mND3FhupQ4/mhHmKWqs7M9wM1w4TYDn8J" +
                    "gat+nyuOxIXJub+cLpAskclujyuOxI3JIcNI4TSObYOo4LEGKf" +
                    "LqSO4wWSObaO4wMUXsczBZI5rNHlcdmRuDU5PMNNNK7NoHFtDn" +
                    "Vkn+GYhCnUTjHInxSOMskwVnYx2SXSmyzgWn2VWaPpXu8Ylede" +
                    "l6eOmEp2OWFabKZZtlep0SyyGzVrNO8wuj42P0u2DzXmua69jt" +
                    "o1jOqYUeM0geSfnLhGc2F34nSyK6V3hYDlz54Sa0SLZwYrtPB+" +
                    "16kjbiKbJxDhGS6WOy9A4TpvFkjn+DyTyOI14pb/o1GXVZOyOW" +
                    "S3Se9WAcvPdYk1Yq5nBivM5f2uU0fcnvzswR1kC8laTZ89eED1" +
                    "C32mtaHP9V1k90jvbgHLz0yJNWKRZwYrLOL9rlPHMnymkO8Kjw" +
                    "ho3mceVaOlxu8zT8Xmlza+RrnGiwIcFwfNvOTN62OS0NAaXxZI" +
                    "5/g8k8gc9voVgXSOzzOJzEHjqwLpHJ9nEpmDxtcE0jk+zySycc" +
                    "41+vjeiFKe67cE0jk+zyQyh71+WyCd4/NMInPQ+I5AOsfnmUTm" +
                    "oHGdQDrH55lEVn+vM2vcULLPFBut0rjJqr1+v/r8mFnjhwlqPp" +
                    "XtD1bU8aNS7fXHVmv8pDozBWjcrPovLdb4mVV1/Lz0nym2lObM" +
                    "bLVE4xfVs6dTNX5lgcavQ/631tfxm+p+LEDjd9Zr/L7aa0ONP1" +
                    "qs8aeGOTM/W6Hxl+rvdSaNvyn2z9LX8fe6avyj+n9hbhr/MtL2" +
                    "N6cR/wHeSjy/");
                
                byte[] buffer = new byte[uncompressedBytes];
                Inflater inflater = new Inflater();
                inflater.setInput(decoded, 0, compressedBytes);
                inflater.inflate(buffer);
                inflater.end();
                
                value = new int[rows][cols];
                for (int index = 0; index < uncompressedBytes-1; index += 4)
                {
                    int byte1 = 0x000000FF & (int)buffer[index + 0];
                    int byte2 = 0x000000FF & (int)buffer[index + 1];
                    int byte3 = 0x000000FF & (int)buffer[index + 2];
                    int byte4 = 0x000000FF & (int)buffer[index + 3];
                    
                    int element = index / 4;
                    int row = element / cols;
                    int col = element % cols;
                    value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
                }
            }
            catch (Exception e)
            {
                throw new Error(e);
            }
        }

        protected static int[][] value1 = null;

        protected static void value1Init()
        {
            try
            {
                final int rows = 120;
                final int cols = 41;
                final int compressedBytes = 525;
                final int uncompressedBytes = 19681;
                
                byte[] decoded = new byte[compressedBytes];
                base64Decode(decoded,
                    "eNrtmudOAlEQhRm7sUaNJfausffesTyCL6fvZIm9a+wtauwGsf" +
                    "wwIOwii3vOZtgECCzhyzlzZ+bOrsdj/uGa+Th++8z/8H4z+/V9" +
                    "wHPmAv3W/1/+gXE+GA0IozHTAgzjIrWOeIxLoIzLJDqugDKuGs" +
                    "bjHsmaWYNhXCfPPRtgjJuwOm5R5J5tynjcIavXu9pTWMK4D854" +
                    "oP3jHxgPKePxiMLrY4p4PAFmPCWJxzM4xnPq/fWFbYyXjqnXVx" +
                    "CM1zrvCYvxhjoebyEZ7zQeI8J4T6TjA4XXj7YxPlHF4zM044tj" +
                    "eopXWxnfHKMjCqPP+eJSHUNlFGHwWqJY14xEQ+kYw6ijxIKtmT" +
                    "iKNRNP6XUCVw6XRK0zJuMxibnvkWTgNZMC53Uqm9eSxtaHS7r2" +
                    "uOEwSgYBYyZFj5uley5zjJLto1wu+wxActTrkOt1ns4prGeUfD" +
                    "xGKcDQUQr5vJYiA7/LbcrhxdS5pwTS61Le3CNlmsPD6nEraGYA" +
                    "lYiMUgU676mmnvfUUOSeWs09hvW6zqtTa1AVhwnisd52HRtY64" +
                    "w0EjA2QebwZjqvW/R+Cgt7s7bP5z7w+WM7o9fSYT+jdP543w2/" +
                    "d+3SdR2BuVkP/DX2XvXaZL3uB+7DB5yyZmQQwushvY/UEh1Hvl" +
                    "+nSeePoyA6jlHOw91kuWfctmtIEw7Yz0zSzB+ngjG63gGk+LeV");
                
                byte[] buffer = new byte[uncompressedBytes];
                Inflater inflater = new Inflater();
                inflater.setInput(decoded, 0, compressedBytes);
                inflater.inflate(buffer);
                inflater.end();
                
                value1 = new int[rows][cols];
                for (int index = 0; index < uncompressedBytes-1; index += 4)
                {
                    int byte1 = 0x000000FF & (int)buffer[index + 0];
                    int byte2 = 0x000000FF & (int)buffer[index + 1];
                    int byte3 = 0x000000FF & (int)buffer[index + 2];
                    int byte4 = 0x000000FF & (int)buffer[index + 3];
                    
                    int element = index / 4;
                    int row = element / cols;
                    int col = element % cols;
                    value1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
                }
            }
            catch (Exception e)
            {
                throw new Error(e);
            }
        }

        protected static int[][] value2 = null;

        protected static void value2Init()
        {
            try
            {
                final int rows = 75;
                final int cols = 41;
                final int compressedBytes = 362;
                final int uncompressedBytes = 12301;
                
                byte[] decoded = new byte[compressedBytes];
                base64Decode(decoded,
                    "eNrtmltPwlAQhJmf5w3FC0QxoqhRUOMVETWId42K6Lv81VIaH0" +
                    "gEQmrJmTnZnqQUysOX3TPT3YUgGP1I/XRXv8/+rvBOu3sfE4O/" +
                    "M2wFMY84jNFV+B6T/IyDFqai8wIzY8g3zR7HvtQz7hmR7rmeY9" +
                    "+PmFXQjBojMuyMmLdcj8aIRV5GLPmiGWQpcp3T9x4NRiw708yK" +
                    "N5rJU2hm1TSTSBzX2BlR8KI2W3fcu2544T1FJkZsis0Atuj7wm" +
                    "3z8BgevvP7WtLTNXYt1+NhRNnZftwTnZHuszHiwGrcsc2aD6Pz" +
                    "GUWNe8QcRxx707ueUDyvT613TSSOFU5GnCvFEVWJXF94MX+s0f" +
                    "Sul6S/sV+JeM+1eXgicayT/+fjxou6p0Hbc92qxBF3TmuKe+We" +
                    "Cw+U3vOoqGs8MTLiWdofX3gY8Wo1Rcw5xduQDDc544h3Wc182H" +
                    "78d03xyciIFmmv8CWd6291D091AKMNcrA=");
                
                byte[] buffer = new byte[uncompressedBytes];
                Inflater inflater = new Inflater();
                inflater.setInput(decoded, 0, compressedBytes);
                inflater.inflate(buffer);
                inflater.end();
                
                value2 = new int[rows][cols];
                for (int index = 0; index < uncompressedBytes-1; index += 4)
                {
                    int byte1 = 0x000000FF & (int)buffer[index + 0];
                    int byte2 = 0x000000FF & (int)buffer[index + 1];
                    int byte3 = 0x000000FF & (int)buffer[index + 2];
                    int byte4 = 0x000000FF & (int)buffer[index + 3];
                    
                    int element = index / 4;
                    int row = element / cols;
                    int col = element % cols;
                    value2[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
                }
            }
            catch (Exception e)
            {
                throw new Error(e);
            }
        }

        protected static int lookupValue(int row, int col)
        {
            if (row <= 119)
                return value[row][col];
            else if (row >= 120 && row <= 239)
                return value1[row-120][col];
            else if (row >= 240)
                return value2[row-240][col];
            else
                throw new IllegalArgumentException("Unexpected location requested in value2 lookup");
        }

        static
        {
            sigmapInit();
            valueInit();
            value1Init();
            value2Init();
        }
    }

    protected static int base64Decode(byte[] decodeIntoBuffer, String encodedString)
    {
        int[] encodedBuffer = new int[4];
        int bytesDecoded = 0;
        int inputLength = encodedString.length();

        if (inputLength % 4 != 0) throw new IllegalArgumentException("Invalid Base64-encoded data (wrong length)");

        for (int inputOffset = 0; inputOffset < inputLength; inputOffset += 4)
        {
            int padding = 0;

            for (int i = 0; i < 4; i++)
            {
                char value = encodedString.charAt(inputOffset + i);
                if (value >= 'A' && value <= 'Z')
                    encodedBuffer[i] = value - 'A';
                else if (value >= 'a' && value <= 'z')
                    encodedBuffer[i] = value - 'a' + 26;
                else if (value >= '0' && value <= '9')
                    encodedBuffer[i] = value - '0' + 52;
                else if (value == '+')
                    encodedBuffer[i] = 62;
                else if (value == '/')
                    encodedBuffer[i] = 63;
                else if (value == '=')
                    { encodedBuffer[i] = 0; padding++; }
                else throw new IllegalArgumentException("Invalid character " + value + " in Base64-encoded data");
            }

            assert 0 <= padding && padding <= 2;

            decodeIntoBuffer[bytesDecoded+0] = (byte)(  ((encodedBuffer[0] & 0x3F) <<  2)
                                                      | ((encodedBuffer[1] & 0x30) >>> 4));
            if (padding < 2)
               decodeIntoBuffer[bytesDecoded+1] = (byte)(  ((encodedBuffer[1] & 0x0F) <<  4)
                                                         | ((encodedBuffer[2] & 0x3C) >>> 2));

            if (padding < 1)
               decodeIntoBuffer[bytesDecoded+2] = (byte)(  ((encodedBuffer[2] & 0x03) <<  6)
                                                         |  (encodedBuffer[3] & 0x3F));

            bytesDecoded += (3 - padding);
        }

        return bytesDecoded;
    }

    protected static final Terminal[] acceptTerminal =
    {
        null,
        Terminal.SKIP,
        null,
        null,
        null,
        null,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.SKIP,
        Terminal.LITERAL_STRING_EXCLAMATION,
        Terminal.LITERAL_STRING_EXCLAMATION_EQUALS,
        null,
        null,
        Terminal.STRING_LITERAL,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Terminal.PRAGMA_ACC,
        Terminal.LITERAL_STRING_PERCENT,
        Terminal.LITERAL_STRING_AMPERSAND,
        Terminal.LITERAL_STRING_AMPERSAND_AMPERSAND,
        null,
        null,
        Terminal.CHARACTER_CONSTANT,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Terminal.LITERAL_STRING_LPAREN,
        Terminal.LITERAL_STRING_RPAREN,
        Terminal.LITERAL_STRING_ASTERISK,
        Terminal.LITERAL_STRING_PLUS,
        Terminal.LITERAL_STRING_PLUS_PLUS,
        Terminal.LITERAL_STRING_COMMA,
        Terminal.LITERAL_STRING_HYPHEN,
        Terminal.LITERAL_STRING_HYPHEN_HYPHEN,
        Terminal.LITERAL_STRING_HYPHEN_GREATERTHAN,
        Terminal.LITERAL_STRING_PERIOD,
        Terminal.FLOATING_CONSTANT,
        null,
        null,
        Terminal.FLOATING_CONSTANT,
        Terminal.FLOATING_CONSTANT,
        null,
        Terminal.LITERAL_STRING_SLASH,
        Terminal.INTEGER_CONSTANT,
        Terminal.FLOATING_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        null,
        null,
        null,
        Terminal.FLOATING_CONSTANT,
        Terminal.FLOATING_CONSTANT,
        null,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        null,
        null,
        null,
        null,
        null,
        Terminal.FLOATING_CONSTANT,
        Terminal.FLOATING_CONSTANT,
        null,
        Terminal.INTEGER_CONSTANT,
        null,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        null,
        null,
        Terminal.FLOATING_CONSTANT,
        Terminal.FLOATING_CONSTANT,
        null,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        null,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.INTEGER_CONSTANT,
        Terminal.LITERAL_STRING_COLON,
        Terminal.LITERAL_STRING_LESSTHAN,
        Terminal.LITERAL_STRING_LESSTHAN_LESSTHAN,
        Terminal.LITERAL_STRING_LESSTHAN_EQUALS,
        null,
        Terminal.LITERAL_STRING_EQUALS_EQUALS,
        Terminal.LITERAL_STRING_GREATERTHAN,
        Terminal.LITERAL_STRING_GREATERTHAN_EQUALS,
        Terminal.LITERAL_STRING_GREATERTHAN_GREATERTHAN,
        Terminal.LITERAL_STRING_QUESTION,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        null,
        null,
        null,
        null,
        null,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_LBRACKET,
        null,
        null,
        null,
        null,
        null,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_RBRACKET,
        Terminal.LITERAL_STRING_CARET,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_ASYNC,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_CACHE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_COLLAPSE,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_COPY,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_COPYIN,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_COPYOUT,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_CREATE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_DATA,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_DECLARE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_DEVICE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_DEVICE_UNDERSCORERESIDENT,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_DEVICEPTR,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_FIRSTPRIVATE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_GANG,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_HOST,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_HOST_UNDERSCOREDATA,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_IF,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_INDEPENDENT,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_KERNELS,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_LOOP,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_MAX,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_MIN,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_NUM_UNDERSCOREGANGS,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_NUM_UNDERSCOREWORKERS,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PARALLEL,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PCOPY,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PCOPYIN,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PCOPYOUT,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PCREATE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PRESENT,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPY,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYIN,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYOUT,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECREATE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_PRIVATE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_REDUCTION,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_SEQ,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_SIZEOF,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_UPDATE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_USE_UNDERSCOREDEVICE,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_VECTOR,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_VECTOR_UNDERSCORELENGTH,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_WAIT,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.IDENTIFIER,
        Terminal.LITERAL_STRING_WORKER,
        Terminal.LITERAL_STRING_VBAR,
        Terminal.LITERAL_STRING_VBAR_VBAR,
        Terminal.LITERAL_STRING_TILDE,
    };
}
