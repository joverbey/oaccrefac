int main() {
    int a, b, c, d;
    for (int i = 0; i < 100; i++) { /*<<<<< 3,1,12,6,fail*/
        a = 5;
        if(a < c){
        	b = 6;
        }
        if(a< d){
        	c = 7;
        }
        d = 8;
    }
}
