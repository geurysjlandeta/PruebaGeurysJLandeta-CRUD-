using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Data.Entity.ModelConfiguration;
using System.Linq;
using System.Web;

namespace PruebaGeurysJLandeta_CRUD_.Models.Data
{
    public class TestInvoiceDbContext : DbContext
    {
        public TestInvoiceDbContext() : base("OracleDbContext")
        {
            Database.SetInitializer<TestInvoiceDbContext>(new CreateDatabaseIfNotExists<TestInvoiceDbContext>());
        }

        //Only for OracleDb, comment to SqlServer
        protected override void OnModelCreating(DbModelBuilder modelBuilder)
        {
            modelBuilder.HasDefaultSchema("DCU");
        }

        //DbSet specify the tables that are into of the db, add a DbSet for each requeried table
        public DbSet<Customer> Customers { get; set; }
        public DbSet<CustomerType> CustomerTypes { get; set; }
        public DbSet<Invoice> Invoices { get; set; }
        public DbSet<InvoiceDetail> InvoiceDetails { get; set; }

    }
}