int main() {
    int a, b, d;

    for (int i = 0; i < 100; i++) { /*<<<<< 4,1,9,1,pass*/
        a = 5;
        int c = 5;
        d = c;
    }

    for (int i = 0; i < 100; i++) {
        b = 7;
        int c = 10;
        d = c + 5;
    }
}
