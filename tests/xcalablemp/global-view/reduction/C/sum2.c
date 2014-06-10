#include<xmp.h>
#include<stdio.h>
#include<stdlib.h>   
#pragma xmp nodes p(4,*)
int procs,id, mask,val, result = 0, i,w;
int main(void)
{
  if(xmp_num_nodes() > 31){
    printf("%s\n","You have to run this program by less than 32 nodes.");
    exit(1);
  }

  procs = xmp_num_nodes();
  id = xmp_num_nodes()-1;
  w=1;
  for(i=0;i<procs;i++){
    w*2;
  }
  for(i=0;i<w;i=i+2){
    mask = 1 << id;
    val =!(i & mask);
#pragma xmp reduction(&:val) 
    if(!val != i){
      result = -1;  // NG
    }
  }

#pragma xmp reduction(+:result)
#pragma xmp task on p(1,1)
  {
    if(result == 0){
      printf("PASS\n");
    }
    else{
      fprintf(stderr, "ERROR\n");
      exit(1);
    }
  }
  return 0;
}