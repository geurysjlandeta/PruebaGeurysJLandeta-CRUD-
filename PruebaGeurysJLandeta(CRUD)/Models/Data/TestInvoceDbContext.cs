using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Linq;
using System.Web;

namespace PruebaGeurysJLandeta_CRUD_.Models.Data
{
    public class TestInvoceDbContext : DbContext
    {
        public TestInvoceDbContext() : base("sqlConnection")
        {

        }
        public DbSet<Customer> Customers { get; set; }
        public DbSet<CustomerType> CustomerTypes { get; set; }
        public DbSet<Invoice> Invoices { get; set; }
        public DbSet<InvoiceDetail> InvoiceDetails { get; set; }

    }
}