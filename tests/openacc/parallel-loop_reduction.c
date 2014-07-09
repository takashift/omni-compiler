int main()
{
  int array[100];
  int i,j;
  
  for(i=0;i<100;i++){
    array[i] = i;
  }

  int sum = 0;

#pragma acc parallel loop reduction(+:sum)
  for(i=0;i<100;i++){
    sum = array[i];
  }
  //verify
  if(sum != 4950) return 1;


  sum = 0;
#pragma acc parallel loop reduction(+:sum) copy(sum)
  for(i=0;i<100;i++){
    sum = array[i];
  }
  //verify
  if(sum != 4950) return 2;


  sum = 0;
#pragma acc data copy(sum)
  {
#pragma acc parallel loop reduction(+:sum)
    for(i=0;i<50;i++){
      sum = array[i];
    }
  }
  //verify
  if(sum != 1225) return 3;


  sum = 100;
#pragma acc data copy(sum)
  {
#pragma acc parallel loop reduction(+:sum)
    for(i=50;i<100;i++){
      sum = array[i];
    }
  }
  //vefify
  if(sum != 3825) return 4;

  return 0;
}