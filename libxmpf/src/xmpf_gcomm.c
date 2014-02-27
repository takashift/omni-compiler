#include "xmpf_internal.h"

_Bool xmpf_test_task_on__(_XMP_object_ref_t **r_desc);
void xmpf_end_task__(void);


//#define DBG 1

//
// reduction
//
void xmpf_reduction__(void *data_addr, int *count, int *datatype, int *op,
		      _XMP_object_ref_t **r_desc)
{
  if (*r_desc){

    if (_XMP_is_entire(*r_desc)){
      if ((*r_desc)->ref_kind == XMP_OBJ_REF_NODES){
	_XMP_reduce_NODES_ENTIRE((*r_desc)->n_desc, data_addr, *count, *datatype, *op);
      }
      else {
	_XMP_reduce_NODES_ENTIRE((*r_desc)->t_desc->onto_nodes, data_addr, *count, *datatype, *op);
      }
    }
    else {
      if (xmpf_test_task_on__(r_desc)){
	_XMP_reduce_NODES_ENTIRE(_XMP_get_execution_nodes(), data_addr, *count, *datatype, *op);
	xmpf_end_task__();
      }
    }

  }
  else {
    _XMP_reduce_NODES_ENTIRE(_XMP_get_execution_nodes(), data_addr, *count, *datatype, *op);
  }

}


//
// bcast
//

void _XMPF_bcast_on_nodes(void *data_addr, int count, int datatype,
			  _XMP_object_ref_t *from_desc, _XMP_object_ref_t *on_desc){

  _XMP_ASSERT(!on_desc || on_desc->ref_kind == XMP_OBJ_REF_NODES);

  size_t size = _XMP_get_datatype_size(datatype);

  int root = 0;

  _XMP_RETURN_IF_SINGLE;

  // calc source nodes number
  if (from_desc){

    if (from_desc->ref_kind != XMP_OBJ_REF_NODES){
      _XMP_fatal("Type of the From and ON clauses must be the same.");
    }

    _XMP_nodes_t *from = from_desc->n_desc;

    if (!from->is_member) {
      _XMP_fatal("broadcast failed, cannot find the source node");
    }

    if (on_desc){

      if (on_desc->n_desc != from){
	_XMP_fatal("Node arrays in the FROM and ON clauses must be the same.");
      }

      int acc_nodes_size = 1;

      for (int i = 0; i < from->dim; i++){
	switch (on_desc->subscript_type[i]){
	case SUBSCRIPT_SCALAR:
	  if (from_desc->REF_INDEX[i] != on_desc->REF_INDEX[i]){
	    _XMP_fatal("A subscript of the from-node must be the same "
		       "with that of the on-node that is a scalar.");
	  }
	  break;
	case SUBSCRIPT_ASTERISK:
	  if ((from_desc)->subscript_type[i] != SUBSCRIPT_ASTERISK){
	    _XMP_fatal("A subscript of the from-node must be '*' "
		       "when the corresponding subscript of on-node is '*'");
	  }
	  break;
	case SUBSCRIPT_TRIPLET:
	case SUBSCRIPT_NONE: {
	  int from_idx = from_desc->REF_INDEX[i];
	  int on_lb = on_desc->REF_LBOUND[i];
	  int on_ub = on_desc->REF_UBOUND[i];
	  int on_st = on_desc->REF_STRIDE[i];
	  if (from_idx < on_lb || from_idx > on_ub || (from_idx - on_lb) % on_st != 0){
	    _XMP_fatal("A subscript of the from-node is out of the on-node bound");
	  }
	  root += (acc_nodes_size * ((from_idx - on_lb) / on_st));
	  acc_nodes_size *= _XMP_M_COUNT_TRIPLETi(on_lb, on_ub, on_st);
	  break;
	}
	}
      }
      
    }
    else {
      int acc_nodes_size = 1;
      for (int i = 0; i < from->dim; i++){
	root += (acc_nodes_size * (from_desc->REF_INDEX[i] - 1));
	acc_nodes_size *= from->info[i].size;
      }
    }

  }
  else { /* no from clause, only check */

    if (on_desc){
      for (int i = 0; i < on_desc->n_desc->dim; i++){
	switch (on_desc->subscript_type[i]){
	case SUBSCRIPT_SCALAR:
	  if (on_desc->REF_INDEX[i] != 1){
	    _XMP_fatal("A subscript of the from-node must be the same "
		       "with that of the on-node that is a scalar.");
	  }
	  break;
	case SUBSCRIPT_ASTERISK:
	  _XMP_fatal("A subscript of the from-node must be '*' "
		     "when the corresponding subscript of on-node is '*'");
	  break;
	case SUBSCRIPT_TRIPLET:
	case SUBSCRIPT_NONE:
	  if (on_desc->REF_LBOUND[i] != 1)
	    _XMP_fatal("A subscript of the from-node is out of the on-node bound");
	  }
	  break;
      }
    }

  }

  _XMP_nodes_t *on;

  if (on_desc){

    if (_XMP_is_entire(on_desc)){
      on = on_desc->n_desc;
      MPI_Bcast(data_addr, count*size, MPI_BYTE, root, *((MPI_Comm *)on->comm));
    }
    else {
      if (xmpf_test_task_on__(&on_desc)){
	on = _XMP_get_execution_nodes();
	MPI_Bcast(data_addr, count*size, MPI_BYTE, root, *((MPI_Comm *)on->comm));
	xmpf_end_task__();
      }
    }

  }
  else {
    on = from_desc ? from_desc->n_desc : _XMP_get_execution_nodes();
    MPI_Bcast(data_addr, count*size, MPI_BYTE, root, *((MPI_Comm *)on->comm));
  }

}


void _XMPF_bcast_on_template(void *data_addr, int count, int datatype,
			     _XMP_object_ref_t *from_desc, _XMP_object_ref_t *on_desc){

  _XMP_ASSERT(on_desc || on_desc->ref_kind == XMP_OBJ_REF_TEMPL);

  size_t size = _XMP_get_datatype_size(datatype);

  _XMP_RETURN_IF_SINGLE;

  _XMP_template_t *from;
  _XMP_template_t *on = on_desc->t_desc;

  int from_idx_in_nodes[_XMP_N_MAX_DIM];
  for (int i = 0; i < _XMP_N_MAX_DIM; i++) from_idx_in_nodes[i] = 0;

  // calc source nodes number
  if (from_desc){

    if (from_desc->ref_kind != XMP_OBJ_REF_TEMPL){
      _XMP_fatal("Type of the From and ON clauses must be the same.");
    }

    from = from_desc->t_desc;

    if (!from->is_owner) {
      _XMP_fatal("broadcast failed, cannot find the source node");
    }

    if (on != from){
      _XMP_fatal("Templates in the FROM and ON clauses must be the same.");
    }

    for (int i = 0; i < from->dim; i++){

      int from_idx;

      switch (on_desc->subscript_type[i]){
      case SUBSCRIPT_SCALAR:
	if (from_desc->REF_INDEX[i] != on_desc->REF_INDEX[i]){
	  _XMP_fatal("A subscript of the from-template must be the same "
		     "with that of the on-template that is a scalar.");
	}
	from_idx = from_desc->REF_INDEX[i];
	break;

      case SUBSCRIPT_ASTERISK:
	if (from_desc->subscript_type[i] != SUBSCRIPT_ASTERISK){
	  _XMP_fatal("A subscript of the from-template must be '*' "
		     "when the corresponding subscript of on-template is '*'");
	}
	from_idx = from->chunk[i].par_lower;
	break;

      case SUBSCRIPT_TRIPLET:
      case SUBSCRIPT_NONE: {
	from_idx = from_desc->REF_INDEX[i];
	int on_lb = on_desc->REF_LBOUND[i];
	int on_ub = on_desc->REF_UBOUND[i];
	int on_st = on_desc->REF_STRIDE[i];
	if (from_idx < on_lb || from_idx > on_ub || (from_idx - on_lb) % on_st != 0){
	  _XMP_fatal("A subscript of the from-template is out of the on-template bound");
	}

	break;
      }
      }

      int j = from->chunk[i].onto_nodes_index;
      if (j != _XMP_N_NO_ONTO_NODES){
	// 0-origin
	from_idx_in_nodes[j] = _XMP_calc_template_owner_SCALAR(from, j, from_idx);
      }

    }
      
  }
  else { /* no from clause, only check */

    for (int i = 0; i < on->dim; i++){
      switch (on_desc->subscript_type[i]){
      case SUBSCRIPT_SCALAR:
	if (on_desc->REF_INDEX[i] != on->info[i].ser_lower){
	  _XMP_fatal("A subscript of the from-node must be the same "
		     "with that of the on-node that is a scalar.");
	}
	break;
      case SUBSCRIPT_ASTERISK:
	_XMP_fatal("A subscript of the from-node must be '*' "
		   "when the corresponding subscript of on-node is '*'");
	break;
      case SUBSCRIPT_TRIPLET:
      case SUBSCRIPT_NONE:
	if (on_desc->REF_LBOUND[i] != on->info[i].ser_lower)
	  _XMP_fatal("A subscript of the from-node is out of the on-node bound");
      }
      break;
    }

  }

  int root = 0;
  _XMP_nodes_t *on_nodes;

  if (_XMP_is_entire(on_desc)){
    on_nodes = on->onto_nodes;

    if (from_desc){
      int acc_nodes_size = 1;
      for (int i = 0; i < on_nodes->dim; i++){
	root += (acc_nodes_size * from_idx_in_nodes[i]);
	acc_nodes_size *= on_nodes->info[i].size;
      }
    }

    MPI_Bcast(data_addr, count*size, MPI_BYTE, root, *((MPI_Comm *)on_nodes->comm));
  }
  else {
    if (xmpf_test_task_on__(&on_desc)){
      on_nodes = _XMP_get_execution_nodes();
      
      if (from_desc){
	int acc_nodes_size = 1;
	for (int i = 0; i < on_nodes->inherit_nodes->dim; i++){
	  if (on_nodes->inherit_info[i].shrink) continue;
	  int inherit_lb = on_nodes->inherit_info[i].lower;
	  int inherit_ub = on_nodes->inherit_info[i].upper;
	  int inherit_st = on_nodes->inherit_info[i].stride;
	  root += (acc_nodes_size * ((from_idx_in_nodes[i] - inherit_lb) / inherit_st));
	  acc_nodes_size *= _XMP_M_COUNT_TRIPLETi(inherit_lb, inherit_ub, inherit_st);
	}
      }

      MPI_Bcast(data_addr, count*size, MPI_BYTE, root, *((MPI_Comm *)on_nodes->comm));
      xmpf_end_task__();
    }
  }

}


void xmpf_bcast__(void *data_addr, int *count, int *datatype,
		  _XMP_object_ref_t **from_desc, _XMP_object_ref_t **on_desc)
{
  if (*on_desc && (*on_desc)->ref_kind == XMP_OBJ_REF_TEMPL)
    _XMPF_bcast_on_template(data_addr, *count, *datatype, *from_desc, *on_desc);
  else
    _XMPF_bcast_on_nodes(data_addr, *count, *datatype, *from_desc, *on_desc);

}


//
// barrier
//
void xmpf_barrier__(_XMP_object_ref_t **desc)
{
  if (*desc){

    if (_XMP_is_entire(*desc)){
      if ((*desc)->ref_kind == XMP_OBJ_REF_NODES){
	_XMP_barrier_NODES_ENTIRE((*desc)->n_desc);
      }
      else {
	_XMP_barrier_NODES_ENTIRE((*desc)->t_desc->onto_nodes);
      }
    }
    else {
      if (xmpf_test_task_on__(desc)){
	_XMP_barrier_EXEC();
	xmpf_end_task__();
      }
    }

  }
  else {
    _XMP_barrier_EXEC();
  }

}
