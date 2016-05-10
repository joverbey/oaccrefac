all: feal4_demo

feal4_initial: 
	pgcc -ta=nvidia -Minfo=accel -O3 -o feal4 feal4_initial.c

feal4_restructured: 
	pgcc -ta=nvidia -Minfo=accel -O3 -o feal4 feal4_restructured.c

feal4_final: 
	pgcc -ta=nvidia -Minfo=accel -O3 -o feal4 feal4_final.c

feal4_demo:
	pgcc -ta=nvidia -Minfo=accel -O3 -o feal4 feal4_demo.c

clean:
	rm feal4
