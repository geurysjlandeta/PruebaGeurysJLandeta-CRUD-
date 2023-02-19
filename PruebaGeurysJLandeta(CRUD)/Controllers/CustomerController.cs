using Microsoft.Ajax.Utilities;
using PruebaGeurysJLandeta_CRUD_.Models;
using PruebaGeurysJLandeta_CRUD_.Models.Data;
using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Linq;
using System.Web;
using System.Web.Mvc;

namespace PruebaGeurysJLandeta_CRUD_.Controllers
{
    public class CustomerController : Controller
    {
        private readonly TestInvoceDbContext _dbContext = new TestInvoceDbContext();
        
        public ActionResult Customer()
        {
            var data = _dbContext.Customers.Where(c => c.Status == true).ToList();
            return View(data);
        }
        public ActionResult AddCustomer()
        {
            Customer customer = new Customer
            {
                Status = true
            };

            return View(customer);
        }
        [HttpPost]
        public ActionResult AddCustomer(Customer customer)
        {
            try
            {
                if (ModelState.IsValid)
                {
                    var datos = _dbContext.Customers.Add(customer);
                    _dbContext.SaveChanges();

                    return Redirect("/Customer/Customer");
                }

                return View(customer);
            }
            catch (Exception ex)
            {
                throw new Exception(ex.Message);
            }
        }

        public ActionResult EditCustomer(int id)
        {
            var data = _dbContext.Customers.Find(id);
            Customer model = new Customer()
            {
                Adress = data.Adress,
                CustName = data.CustName,
                CustomerTypeId = data.CustomerTypeId,
                Status = data.Status,
                Id = id
            };

            return View(model);
        }

        [HttpPost]
        public ActionResult EditCustomer(Customer customer)
        {
            try
            {
                if (ModelState.IsValid)
                {
                    var data = _dbContext.Customers.Find(customer.Id);

                    data.Adress = customer.Adress;
                    data.CustName = customer.CustName;
                    data.CustomerTypeId = customer.CustomerTypeId;
                    data.Status = customer.Status;
                    data.Id = customer.Id;
                    
                    _dbContext.Entry(data).State = EntityState.Modified;
                    _dbContext.SaveChanges();

                    return Redirect("/Customer/Customer");
                }

                return View(customer);
            }
            catch (Exception ex)
            {
                throw new Exception(ex.Message);
            }
        }


        [HttpGet]
        public ActionResult DeleteCustomer(Customer customer)
        {
            try
            {
                var data = _dbContext.Customers.Find(customer.Id);

                data.Status = false;

                _dbContext.Entry(data).State = EntityState.Modified;
                _dbContext.SaveChanges();

                return Redirect("/Customer/Customer");
            }
            catch (Exception ex)
            {
                throw new Exception(ex.Message);
            }
        }
    }
}