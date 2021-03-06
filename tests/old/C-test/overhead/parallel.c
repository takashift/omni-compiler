static char rcsid[] = "$Id$";
/* 
 * $TSUKUBA_Release: Omni OpenMP Compiler 3 $
 * $TSUKUBA_Copyright:
 *  PLEASE DESCRIBE LICENSE AGREEMENT HERE
 *  $
 */
/*
 * Test program to measure the overhead of OpenMP parallel
 *     from OdinMP/CCp report by Christian Brunschen at Lund
 *     modified 99/10/6 by K. Kusano at RWCP
 */

#include <sys/time.h>
/*#include <unistd.h>*/
#include <stdlib.h>
#include <strings.h>

int spin_factor = 1000;

void * nop(void * p)
{
    return p;
}

void spin(double jLimit)
{
    int  i;
    double  j;
    double  d;

    for( i = 0 ; i < spin_factor ; i++ ){
	for( j = 0.0 ; j < jLimit ; j += 1.0 ){
	    nop(&d);
	}
    }
}

double t()
{
    struct timeval tv;

    gettimeofday(&tv, ((void *) 0));
    return (double)tv.tv_sec + (double)tv.tv_usec / 1000000.0;
}

int main(int argc, char ** argv)
{
    int  i, j;
    int  num = 100, bign = 10;
    double  t0, t1, t2, t3, dt1t0, dt3t2;
    double  dt1t0_iter, dt3t2_iter;
    double  diff, diff_iter;

    for( i = 1 ; i < argc ; i++ ){
	if ( !strcmp(argv[i], "-n") ){
	    if ( argc <= (i+1) ){
		fprintf(stderr, "need argument to '-n'\n");
		exit(1);
	    }
	    num = atoi(argv[++i]);
	}
	else if ( !strcmp(argv[i], "-f") ){
	    if ( argc <= (i+1) ){
		fprintf(stderr, "need argument to '-f'\n");
		exit(1);
	    }
	    spin_factor = atoi(argv[++i]);
	}
	else if ( !strcmp(argv[i], "-b") ){
	    if ( argc <= (i+1) ){
		fprintf(stderr, "need argument to '-b'\n");
		exit(1);
	    }
	    bign = atoi(argv[++i]);
	}
	else if ( !strcmp(argv[i], "-h") ){
	    printf("Usage: %s [option]\n", argv[0]);
	    printf("  -b: # of measurement [10]\n");
	    printf("  -n: # of parallel invocation [100]\n");
	    printf("  -f: # of spin factor [1000]\n");
	    printf("  -h: show this message\n");
	    exit(0);
	}
    }

    dt1t0_iter = dt3t2_iter = 10000.0;

    for( j = 0 ; j < bign ; j++ ){

	printf("Parallel[%d]   ", j);
	t0 = t();
	for( i = 0 ; i < num ; i++ ){
#pragma omp parallel
	    spin(1000.0);
	}
	t1 = t();
	dt1t0 = t1 - t0;
	printf("%15.9g\n", dt1t0);
	if ( dt1t0_iter > dt1t0 ){
	    dt1t0_iter = dt1t0;
	}

	printf("Sequential[%d] ", j);
	t2 = t();
	for( i = 0 ; i < num ; i++ ){
	    spin(1000.0);
	}
	t3 = t();
	dt3t2 = t3 - t2;
	printf("%15.9g\n", dt3t2);
	if ( dt3t2_iter > dt3t2 ){
	    dt3t2_iter = dt3t2;
	}
    }

    dt1t0 = dt1t0_iter;
    dt3t2 = dt3t2_iter;

    dt1t0_iter = dt1t0 / num;
    printf("Parallel(best):\n");
    printf("time              : %15.9g s = %15.9g ms = %15.9g us\n",
	   dt1t0, dt1t0*1000.0, dt1t0*1000000.0);
    printf("time/iteration    : %15.9g s = %15.9g ms = %15.9g us\n",
	   dt1t0_iter, dt1t0_iter*1000.0, dt1t0_iter*1000000.0);

    dt3t2_iter = dt3t2 / num;
    printf("Sequential(best):\n");
    printf("time              : %15.9g s = %15.9g ms = %15.9g us\n",
	   dt3t2, dt3t2*1000.0, dt3t2*1000000.0);
    printf("time/iteration    : %15.9g s = %15.9g ms = %15.9g us\n",
	   dt3t2_iter, dt3t2_iter*1000.0, dt3t2_iter*1000000.0);

    diff = dt1t0 - dt3t2;
    diff_iter = dt1t0_iter - dt3t2_iter;
    printf("OpenMP verhead:\n");
    printf("overhead          : %15.9g s = %15.9g ms = %15.9g us\n",
	   diff, diff*1000.0, diff*1000000.0);
    printf("overhead/iteration: %15.9g s = %15.9g ms = %15.9g us\n",
	   diff_iter, diff_iter*1000.0, diff_iter*1000000.0);
}
