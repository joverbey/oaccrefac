int main() {
    int a, b;
    float d;
    int c_0 = 12;

    for (int i = 0; i < 100; i++) { /*<<<<< 6,1,11,1,fail*/
        a = 5;
        int c = 5;
        d = c;
    }

    for (int i = 0; i < 100; i++) {
    	float c = 10.2;
    	b = 7;
    }
}
