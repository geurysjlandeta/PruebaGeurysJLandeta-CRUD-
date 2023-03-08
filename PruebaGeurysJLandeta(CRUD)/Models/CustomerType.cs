using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Web;

namespace PruebaGeurysJLandeta_CRUD_.Models
{
    [Table("CustomerTypes")]
    public class CustomerType
    {
        public int Id { get; set; }
        public string Description { get; set; }
    }
}