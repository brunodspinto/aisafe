grammar FlightPlanDsl;

flightPlan
    : flight EOF
    ;

flight
    : FLIGHT IDENTIFIER TYPE flightType LBRACE leg+ RBRACE
    ;

flightType
    : REGULAR
    | CHARTER
    ;

leg
    : LEG LBRACE departure arrival route segment+ fuel RBRACE
    ;

departure
    : DEPARTURE airportCode dateTime SEMI
    ;

arrival
    : ARRIVAL airportCode dateTime SEMI
    ;

route
    : ROUTE airportCode ARROW airportCode SEMI
    ;

segment
    : SEGMENT coordinate ARROW coordinate ALTITUDE NUMBER M WIDTH NUMBER M WIND NUMBER NUMBER MPS SEMI
    ;

fuel
    : FUEL NUMBER fuelUnit SEMI
    ;

fuelUnit
    : KG
    | L
    ;

airportCode
    : IATA_CODE
    | ICAO_CODE
    ;

coordinate
    : LPAREN signedNumber COMMA signedNumber RPAREN
    ;

signedNumber
    : (PLUS | MINUS)? NUMBER
    ;

dateTime
    : DATE TIME
    ;

FLIGHT: F L I G H T;
TYPE: T Y P E;
REGULAR: R E G U L A R;
CHARTER: C H A R T E R;
LEG: L E G;
DEPARTURE: D E P A R T U R E;
ARRIVAL: A R R I V A L;
ROUTE: R O U T E;
SEGMENT: S E G M E N T;
FUEL: F U E L;
ALTITUDE: A L T I T U D E;
WIDTH: W I D T H;
WIND: W I N D;
KG: K G;
MPS: M '/' S;

ARROW: '->';
LBRACE: '{';
RBRACE: '}';
LPAREN: '(';
RPAREN: ')';
SEMI: ';';
COMMA: ',';
PLUS: '+';
MINUS: '-';
M: M_CHAR;
L: L_CHAR;

IATA_CODE: UPPER UPPER UPPER;
ICAO_CODE: UPPER UPPER UPPER UPPER;
DATE: DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT;
TIME: DIGIT DIGIT ':' DIGIT DIGIT;
NUMBER: DIGIT+ ('.' DIGIT+)?;
IDENTIFIER: LETTER (LETTER | DIGIT | '_' | '-')*;

WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment UPPER: [A-Z];
fragment LETTER: [a-zA-Z];

fragment A: [aA];
fragment B: [bB];
fragment C: [cC];
fragment D: [dD];
fragment E: [eE];
fragment F: [fF];
fragment G: [gG];
fragment H: [hH];
fragment I: [iI];
fragment J: [jJ];
fragment K: [kK];
fragment L_CHAR: [lL];
fragment M_CHAR: [mM];
fragment N: [nN];
fragment O: [oO];
fragment P: [pP];
fragment Q: [qQ];
fragment R: [rR];
fragment S: [sS];
fragment T: [tT];
fragment U: [uU];
fragment V: [vV];
fragment W: [wW];
fragment X: [xX];
fragment Y: [yY];
fragment Z: [zZ];

